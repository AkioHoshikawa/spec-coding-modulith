package com.example.modulith.poc.channel.web.dto.order;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 注文レスポンス
 */
public record OrderResponse(
        UUID orderId,
        String orderNumber,
        UUID userId,
        String status,
        String paymentStatus,
        List<OrderItemResponse> items,
        BigDecimal subtotal,
        BigDecimal tax,
        BigDecimal shippingFee,
        BigDecimal discount,
        BigDecimal totalAmount,
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
}
