package com.example.modulith.poc.event.inventory;

import com.example.modulith.poc.core.event.EventBase;
import com.example.modulith.poc.core.event.EventHeader;

import java.util.List;
import java.util.UUID;

/**
 * 在庫ロック失敗イベント
 * <p>
 * 在庫不足や楽観ロック競合などで在庫ロックが失敗した際に発行されるイベント。
 * エラー詳細情報を含む。
 */
public final class InventoryLockFailed extends EventBase {

    private final UUID orderId;
    private final List<InventoryError> errors;

    public InventoryLockFailed(
            EventHeader header,
            UUID orderId,
            List<InventoryError> errors
    ) {
        super(header);
        this.orderId = orderId;
        this.errors = errors;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public List<InventoryError> getErrors() {
        return errors;
    }

    /**
     * 在庫エラー情報
     */
    public record InventoryError(
            UUID skuId,
            String errorCode,
            String message,
            Integer requestedQuantity,
            Integer availableQuantity
    ) {
    }
}
