package com.example.modulith.poc.model.inventory.exception;

import java.util.UUID;

/**
 * 在庫不足例外
 * <p>
 * 在庫が不足していて注文を処理できない場合にスローされる。
 */
public class InsufficientInventoryException extends RuntimeException {

    private final UUID skuId;
    private final Integer requested;
    private final Integer available;

    public InsufficientInventoryException(UUID skuId, Integer requested, Integer available) {
        super(String.format("在庫が不足しています: SKU=%s, 要求数=%d, 利用可能数=%d",
                skuId, requested, available));
        this.skuId = skuId;
        this.requested = requested;
        this.available = available;
    }

    public UUID getSkuId() {
        return skuId;
    }

    public Integer getRequested() {
        return requested;
    }

    public Integer getAvailable() {
        return available;
    }
}
