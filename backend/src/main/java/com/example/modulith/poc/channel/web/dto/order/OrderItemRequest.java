package com.example.modulith.poc.channel.web.dto.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * 注文アイテムリクエスト
 */
public record OrderItemRequest(
        @NotNull(message = "SKU IDは必須です")
        UUID skuId,

        @NotNull(message = "数量は必須です")
        @Min(value = 1, message = "数量は1以上である必要があります")
        Integer quantity
) {
}
