package com.example.modulith.poc.event.order;

import com.example.modulith.poc.core.event.EventBase;
import com.example.modulith.poc.core.event.EventHeader;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 注文作成完了イベント
 * <p>
 * 注文作成処理が完了した際に発行されるイベント。
 * 完成した注文の詳細情報を含む。
 */
public final class OrderCreateComplete extends EventBase {

    private final UUID orderId;
    private final String orderNumber;
    private final UUID userId;
    private final String orderStatus;
    private final String paymentStatus;
    private final List<OrderItemData> items;
    private final BigDecimal subtotal;
    private final BigDecimal tax;
    private final BigDecimal shippingFee;
    private final BigDecimal discount;
    private final BigDecimal totalAmount;
    private final UUID shippingAddressId;
    private final String recipientName;
    private final String recipientPhone;
    private final String shippingPostalCode;
    private final String shippingPrefecture;
    private final String shippingCity;
    private final String shippingAddressLine1;
    private final String shippingAddressLine2;
    private final String paymentMethod;
    private final String notes;
    private final OffsetDateTime orderedAt;
    private final OffsetDateTime createdAt;

    public OrderCreateComplete(
            EventHeader header,
            UUID orderId,
            String orderNumber,
            UUID userId,
            String orderStatus,
            String paymentStatus,
            List<OrderItemData> items,
            BigDecimal subtotal,
            BigDecimal tax,
            BigDecimal shippingFee,
            BigDecimal discount,
            BigDecimal totalAmount,
            UUID shippingAddressId,
            String recipientName,
            String recipientPhone,
            String shippingPostalCode,
            String shippingPrefecture,
            String shippingCity,
            String shippingAddressLine1,
            String shippingAddressLine2,
            String paymentMethod,
            String notes,
            OffsetDateTime orderedAt,
            OffsetDateTime createdAt
    ) {
        super(header);
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.orderStatus = orderStatus;
        this.paymentStatus = paymentStatus;
        this.items = items;
        this.subtotal = subtotal;
        this.tax = tax;
        this.shippingFee = shippingFee;
        this.discount = discount;
        this.totalAmount = totalAmount;
        this.shippingAddressId = shippingAddressId;
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.shippingPostalCode = shippingPostalCode;
        this.shippingPrefecture = shippingPrefecture;
        this.shippingCity = shippingCity;
        this.shippingAddressLine1 = shippingAddressLine1;
        this.shippingAddressLine2 = shippingAddressLine2;
        this.paymentMethod = paymentMethod;
        this.notes = notes;
        this.orderedAt = orderedAt;
        this.createdAt = createdAt;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public List<OrderItemData> getItems() {
        return items;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getTax() {
        return tax;
    }

    public BigDecimal getShippingFee() {
        return shippingFee;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public UUID getShippingAddressId() {
        return shippingAddressId;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public String getRecipientPhone() {
        return recipientPhone;
    }

    public String getShippingPostalCode() {
        return shippingPostalCode;
    }

    public String getShippingPrefecture() {
        return shippingPrefecture;
    }

    public String getShippingCity() {
        return shippingCity;
    }

    public String getShippingAddressLine1() {
        return shippingAddressLine1;
    }

    public String getShippingAddressLine2() {
        return shippingAddressLine2;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getNotes() {
        return notes;
    }

    public OffsetDateTime getOrderedAt() {
        return orderedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 注文アイテムデータ
     */
    public record OrderItemData(
            UUID orderLineId,
            UUID skuId,
            String productName,
            String skuCode,
            String color,
            String size,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal discountAmount,
            BigDecimal lineTotal,
            UUID inventoryLockId
    ) {
    }
}
