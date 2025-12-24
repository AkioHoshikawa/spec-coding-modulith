package com.example.modulith.poc.channel.web.dto.order;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 注文アイテムレスポンス
 */
public record OrderItemResponse(
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
