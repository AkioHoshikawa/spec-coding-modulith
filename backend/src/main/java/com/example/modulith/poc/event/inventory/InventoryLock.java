package com.example.modulith.poc.event.inventory;

import com.example.modulith.poc.core.event.EventBase;
import com.example.modulith.poc.core.event.EventHeader;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * 在庫ロックイベント
 * <p>
 * 注文確定時に複数SKUの在庫をロックするためのイベント。
 * 在庫モジュールがこのイベントを受信して在庫引当処理を実行する。
 */
public final class InventoryLock extends EventBase {

    /**
     * 注文ID
     */
    private final @NotNull UUID orderId;

    /**
     * ロック対象アイテムリスト
     */
    private final @NotNull
    @NotEmpty
    @Valid List<InventoryLockItem> items;

    public InventoryLock(
            EventHeader header,
            UUID orderId,
            List<InventoryLockItem> items
    ) {
        super(header);
        this.orderId = orderId;
        this.items = items;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public List<InventoryLockItem> getItems() {
        return items;
    }

    /**
     * 在庫ロックアイテム
     */
    public record InventoryLockItem(
            @NotNull UUID skuId,
            @NotNull @Min(1) Integer quantity
    ) {
    }
}
