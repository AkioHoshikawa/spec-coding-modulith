package com.example.modulith.poc.channel.web.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * 注文作成リクエスト
 */
public record CreateOrderRequest(
        @NotNull(message = "注文アイテムは必須です")
        @NotEmpty(message = "注文アイテムは1つ以上必要です")
        @Valid
        List<OrderItemRequest> items,

        @NotNull(message = "配送先住所IDは必須です")
        UUID shippingAddressId,

        UUID billingAddressId,

        @NotNull(message = "支払い方法は必須です")
        String paymentMethod,

        String couponCode,

        String notes
) {
}
