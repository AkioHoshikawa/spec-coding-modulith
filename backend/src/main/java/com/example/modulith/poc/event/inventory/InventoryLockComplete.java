package com.example.modulith.poc.event.inventory;

import com.example.modulith.poc.core.event.EventBase;
import com.example.modulith.poc.core.event.EventHeader;

import java.util.List;
import java.util.UUID;

/**
 * 在庫ロック完了イベント
 * <p>
 * 在庫ロック処理が成功した際に発行されるイベント。
 * 各SKUの在庫ロックID情報を含む。
 */
public final class InventoryLockComplete extends EventBase {

    private final UUID orderId;
    private final List<InventoryLockResult> results;

    public InventoryLockComplete(
            EventHeader header,
            UUID orderId,
            List<InventoryLockResult> results
    ) {
        super(header);
        this.orderId = orderId;
        this.results = results;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public List<InventoryLockResult> getResults() {
        return results;
    }

    /**
     * 在庫ロック結果
     */
    public record InventoryLockResult(
            UUID skuId,
            UUID inventoryLockId,
            Integer lockedQuantity
    ) {
    }
}
