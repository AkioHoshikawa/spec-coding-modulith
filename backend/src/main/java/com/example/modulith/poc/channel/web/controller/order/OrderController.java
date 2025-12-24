package com.example.modulith.poc.channel.web.controller.order;

import com.example.modulith.poc.channel.web.controller.EventCoordinatingController;
import com.example.modulith.poc.channel.web.dto.order.CreateOrderRequest;
import com.example.modulith.poc.channel.web.dto.order.OrderItemResponse;
import com.example.modulith.poc.channel.web.dto.order.OrderResponse;
import com.example.modulith.poc.channel.web.exception.EventErrorException;
import com.example.modulith.poc.channel.web.service.EventCoordinatingService;
import com.example.modulith.poc.core.event.EventBase;
import com.example.modulith.poc.core.event.EventHeader;
import com.example.modulith.poc.event.order.OrderCreate;
import com.example.modulith.poc.event.order.OrderCreateComplete;
import com.example.modulith.poc.core.service.IdempotencyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

/**
 * 注文コントローラー
 * <p>
 * 注文関連のAPIエンドポイントを提供する。
 */
@RestController
@RequestMapping("/v1/orders")
public class OrderController extends EventCoordinatingController {

    @Autowired
    public OrderController(EventCoordinatingService eventCoordinatingService, IdempotencyService idempotencyService) {
        super(eventCoordinatingService);
    }

    /**
     * 注文作成
     * <p>
     * カートから注文を作成し、在庫を引き当てる。
     *
     * @param idempotencyKey 冪等性キー
     * @param request        注文作成リクエスト
     * @return 注文レスポンス
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Mono<OrderResponse> createOrder(
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request) {

        // TODO: 冪等性関連の実装

        // OrderCreateイベント作成・発行
        var event = new OrderCreate(
                new EventHeader("user1"), // TODO: 認証情報から取得
                idempotencyKey,
                request.items().stream()
                        .map(item -> new OrderCreate.OrderItemData(item.skuId(), item.quantity()))
                        .collect(Collectors.toList()),
                request.shippingAddressId(),
                request.billingAddressId(),
                request.paymentMethod(),
                request.couponCode(),
                request.notes()
        );

        return super.<OrderCreateComplete>publishEvent(event)
                .asMono()
                .doOnError(EventErrorException.class, e -> {
                    throw e;
                })
                .map(this::toResponse);
    }

    /**
     * イベント完了通知を受信
     */
    @EventListener({OrderCreateComplete.class})
    public void onEventComplete(EventBase event) {
        emitResponse(event);
    }

    /**
     * OrderCreateCompleteイベントをOrderResponseに変換
     */
    private OrderResponse toResponse(OrderCreateComplete event) {
        var items = event.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.orderLineId(),
                        item.skuId(),
                        item.productName(),
                        item.skuCode(),
                        item.color(),
                        item.size(),
                        item.quantity(),
                        item.unitPrice(),
                        item.discountAmount(),
                        item.lineTotal(),
                        item.inventoryLockId()
                ))
                .collect(Collectors.toList());

        return new OrderResponse(
                event.getOrderId(),
                event.getOrderNumber(),
                event.getUserId(),
                event.getOrderStatus(),
                event.getPaymentStatus(),
                items,
                event.getSubtotal(),
                event.getTax(),
                event.getShippingFee(),
                event.getDiscount(),
                event.getTotalAmount(),
                event.getRecipientName(),
                event.getRecipientPhone(),
                event.getShippingPostalCode(),
                event.getShippingPrefecture(),
                event.getShippingCity(),
                event.getShippingAddressLine1(),
                event.getShippingAddressLine2(),
                event.getPaymentMethod(),
                event.getNotes(),
                event.getOrderedAt(),
                event.getCreatedAt()
        );
    }
}
