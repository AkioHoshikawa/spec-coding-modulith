package com.example.modulith.poc.event.order;

import com.example.modulith.poc.core.event.EventBase;
import com.example.modulith.poc.core.event.EventHeader;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * 注文作成イベント
 * <p>
 * カートから注文を作成する際に発行されるイベント。
 * 複数SKUの注文アイテム、配送先情報、支払い方法などを含む。
 */
public final class OrderCreate extends EventBase {

    /**
     * 冪等性キー（重複リクエスト防止用）
     */
    private final @NotBlank String idempotencyKey;

    /**
     * 注文アイテムリスト
     */
    private final @NotNull
    @NotEmpty
    @Valid List<OrderItemData> items;

    /**
     * 配送先住所ID
     */
    private final @NotNull UUID shippingAddressId;

    /**
     * 請求先住所ID（省略時は配送先と同じ）
     */
    private final UUID billingAddressId;

    /**
     * 支払い方法
     */
    private final @NotBlank String paymentMethod;

    /**
     * クーポンコード
     */
    private final String couponCode;

    /**
     * 注文メモ
     */
    private final String notes;

    public OrderCreate(
            EventHeader header,
            String idempotencyKey,
            List<OrderItemData> items,
            UUID shippingAddressId,
            UUID billingAddressId,
            String paymentMethod,
            String couponCode,
            String notes
    ) {
        super(header);
        this.idempotencyKey = idempotencyKey;
        this.items = items;
        this.shippingAddressId = shippingAddressId;
        this.billingAddressId = billingAddressId;
        this.paymentMethod = paymentMethod;
        this.couponCode = couponCode;
        this.notes = notes;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public List<OrderItemData> getItems() {
        return items;
    }

    public UUID getShippingAddressId() {
        return shippingAddressId;
    }

    public UUID getBillingAddressId() {
        return billingAddressId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public String getNotes() {
        return notes;
    }

    /**
     * 注文アイテムデータ
     */
    public record OrderItemData(
            @NotNull UUID skuId,
            @NotNull Integer quantity
    ) {
    }
}
