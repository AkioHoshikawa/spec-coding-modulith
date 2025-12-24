package com.example.modulith.poc.model.order.service;

import com.example.modulith.poc.core.event.EventHeader;
import com.example.modulith.poc.event.inventory.InventoryLockComplete;
import com.example.modulith.poc.event.inventory.InventoryLockFailed;
import com.example.modulith.poc.event.order.OrderCreate;
import com.example.modulith.poc.event.order.OrderCreateComplete;
import com.example.modulith.poc.model.order.entity.OrderEntity;
import com.example.modulith.poc.model.order.entity.OrderLineEntity;
import com.example.modulith.poc.model.order.entity.OrderStatus;
import com.example.modulith.poc.model.order.entity.PaymentStatus;
import com.example.modulith.poc.model.order.repository.OrderLineRepository;
import com.example.modulith.poc.model.order.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 注文サービス実装
 */
@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderLineRepository orderLineRepository;
    private final AtomicInteger orderSequence = new AtomicInteger(1);

    @Autowired
    public OrderServiceImpl(OrderRepository orderRepository, OrderLineRepository orderLineRepository) {
        this.orderRepository = orderRepository;
        this.orderLineRepository = orderLineRepository;
    }

    @Override
    @Transactional
    public OrderEntity createOrder(OrderCreate event) {
        // 注文エンティティを作成
        OrderEntity order = new OrderEntity();
        order.setOrderNumber(generateOrderNumber());
        // TODO: 実際は認証情報から取得したユーザーIDを使用
        // 暫定的にランダムUUIDを設定（テスト用）
        order.setUserId(UUID.randomUUID());
        order.setOrderStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);

        // 仮の金額設定（実際は商品情報から計算）
        order.setSubtotalAmount(BigDecimal.ZERO);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setShippingFee(BigDecimal.valueOf(500)); // 固定送料
        order.setTaxAmount(BigDecimal.ZERO);
        order.setTotalAmount(BigDecimal.ZERO);

        // 配送先情報設定（実際はUserAddressから取得）
        order.setShippingAddressId(event.getShippingAddressId());
        order.setRecipientName("受取人名"); // TODO: 実際の住所情報から取得
        order.setRecipientPhone("000-0000-0000");
        order.setShippingPostalCode("000-0000");
        order.setShippingPrefecture("東京都");
        order.setShippingCity("千代田区");
        order.setShippingAddressLine1("丸の内1-1-1");
        order.setShippingAddressLine2("");

        // 支払い方法設定
        order.setPaymentMethod(event.getPaymentMethod());

        // 注文メモ
        if (event.getNotes() != null) {
            order.setCustomerNote(event.getNotes());
        }

        // 注文日時
        order.setOrderedAt(OffsetDateTime.now());

        // 保存
        order = orderRepository.save(order);

        // 注文明細を作成
        BigDecimal subtotal = BigDecimal.ZERO;
        int lineNumber = 1;
        for (OrderCreate.OrderItemData item : event.getItems()) {
            OrderLineEntity orderLine = new OrderLineEntity();
            orderLine.setOrder(order);
            orderLine.setLineNumber(lineNumber++);
            orderLine.setSkuId(item.skuId());

            // TODO: 実際はSKUマスタから商品情報を取得
            orderLine.setProductName("商品名");
            orderLine.setSkuCode("SKU-CODE");
            orderLine.setColor("色");
            orderLine.setSize("サイズ");
            orderLine.setQuantity(item.quantity());

            // 仮の価格設定
            BigDecimal unitPrice = BigDecimal.valueOf(5000);
            orderLine.setUnitPrice(unitPrice);
            orderLine.setDiscountAmount(BigDecimal.ZERO);
            orderLine.setLineTotal(unitPrice.multiply(BigDecimal.valueOf(item.quantity())));

            orderLineRepository.save(orderLine);

            subtotal = subtotal.add(orderLine.getLineTotal());
        }

        // 金額再計算
        BigDecimal taxRate = BigDecimal.valueOf(0.10); // 10%
        BigDecimal tax = subtotal.multiply(taxRate);
        BigDecimal total = calculateTotalAmount(subtotal, tax, order.getShippingFee(), order.getDiscountAmount());

        order.setSubtotalAmount(subtotal);
        order.setTaxAmount(tax);
        order.setTotalAmount(total);

        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public OrderCreateComplete onInventoryLockComplete(InventoryLockComplete event) {
        // 注文を取得して更新
        OrderEntity order = findById(event.getOrderId());

        order.setOrderStatus(OrderStatus.CONFIRMED);
        order.setConfirmedAt(java.time.OffsetDateTime.now());
        orderRepository.save(order);

        // 注文明細に在庫ロックIDを設定
        List<OrderLineEntity> orderLines = orderLineRepository.findByOrder_OrderIdOrderByLineNumber(event.getOrderId());
        for (int i = 0; i < orderLines.size() && i < event.getResults().size(); i++) {
            OrderLineEntity orderLine = orderLines.get(i);
            InventoryLockComplete.InventoryLockResult lockResult = event.getResults().get(i);
            orderLine.setInventoryLockId(lockResult.inventoryLockId());
            orderLineRepository.save(orderLine);
        }

        // OrderCreateCompleteイベント発行
        return buildCompleteEvent(event.getHeader(), order, orderLines);
    }

    @Override
    @Transactional
    public OrderCreateComplete onInventoryLockFailed(InventoryLockFailed event) {
        // 注文をキャンセル
        OrderEntity order = findById(event.getOrderId());

        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(java.time.OffsetDateTime.now());
        order.setCancellationReason("在庫不足");
        orderRepository.save(order);

        // エラーフラグをtrueにした状態でCompleteイベント発行
        EventHeader errorHeader = new EventHeader(true, event.getHeader().getTxId(), event.getHeader().getUserId());
        List<OrderLineEntity> orderLines = orderLineRepository.findByOrder_OrderIdOrderByLineNumber(event.getOrderId());
        return buildCompleteEvent(errorHeader, order, orderLines);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderEntity findById(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("注文が見つかりません: " + orderId));
    }

    private String generateOrderNumber() {
        // ORD-YYYYMMDD-xxxxx形式
        String dateStr = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int sequence = orderSequence.getAndIncrement();
        return String.format("ORD-%s-%05d", dateStr, sequence);
    }


    private BigDecimal calculateTotalAmount(BigDecimal subtotal, BigDecimal tax, BigDecimal shippingFee, BigDecimal discount) {
        return subtotal.add(tax).add(shippingFee).subtract(discount);
    }

    private OrderCreateComplete buildCompleteEvent(EventHeader header, OrderEntity order, List<OrderLineEntity> orderLines) {
        List<OrderCreateComplete.OrderItemData> items = orderLines.stream()
                .map(line -> new OrderCreateComplete.OrderItemData(
                        line.getOrderLineId(),
                        line.getSkuId(),
                        line.getProductName(),
                        line.getSkuCode(),
                        line.getColor(),
                        line.getSize(),
                        line.getQuantity(),
                        line.getUnitPrice(),
                        line.getDiscountAmount(),
                        line.getLineTotal(),
                        line.getInventoryLockId()
                ))
                .collect(Collectors.toList());

        return new OrderCreateComplete(
                header,
                order.getOrderId(),
                order.getOrderNumber(),
                order.getUserId(),
                order.getOrderStatus().name(),
                order.getPaymentStatus().name(),
                items,
                order.getSubtotalAmount(),
                order.getTaxAmount(),
                order.getShippingFee(),
                order.getDiscountAmount(),
                order.getTotalAmount(),
                order.getShippingAddressId(),
                order.getRecipientName(),
                order.getRecipientPhone(),
                order.getShippingPostalCode(),
                order.getShippingPrefecture(),
                order.getShippingCity(),
                order.getShippingAddressLine1(),
                order.getShippingAddressLine2(),
                order.getPaymentMethod(),
                order.getCustomerNote(),
                order.getOrderedAt(),
                order.getCreatedAt()
        );
    }
}
