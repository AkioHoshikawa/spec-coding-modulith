package com.example.modulith.poc.model.inventory.service;

import com.example.modulith.poc.event.inventory.InventoryLock;

import java.util.List;
import java.util.UUID;

/**
 * 在庫サービスインターフェース
 * <p>
 * 在庫のロック・解放・トランザクション記録を管理する。
 */
public interface InventoryService {

    /**
     * 在庫をロックする（単一SKU）
     *
     * @param orderId  注文ID
     * @param skuId    SKU ID
     * @param quantity ロック数量
     * @return 在庫ロック結果
     * @throws com.example.modulith.poc.model.inventory.exception.InsufficientInventoryException 在庫不足の場合
     * @throws jakarta.persistence.OptimisticLockException                                       楽観ロック競合の場合
     */
    InventoryLockResult lockInventory(UUID orderId, UUID skuId, Integer quantity);

    /**
     * 複数SKUの在庫をロックする
     *
     * @param orderId 注文ID
     * @param items   ロック対象アイテムリスト
     * @return 在庫ロック結果リスト
     */
    List<InventoryLockResult> lockMultipleInventory(UUID orderId, List<InventoryLock.InventoryLockItem> items);

    /**
     * 在庫ロックを解放する
     *
     * @param orderId  注文ID
     * @param skuId    SKU ID
     * @param quantity 解放数量
     */
    void unlockInventory(UUID orderId, UUID skuId, Integer quantity);

    /**
     * 在庫トランザクションを記録する
     *
     * @param skuId           SKU ID
     * @param transactionType トランザクション種別
     * @param quantityChange  数量変化
     * @param quantityBefore  変更前数量
     * @param quantityAfter   変更後数量
     * @param referenceId     参照ID
     * @param reason          理由
     */
    void recordTransaction(UUID skuId, String transactionType, Integer quantityChange,
                           Integer quantityBefore, Integer quantityAfter, UUID referenceId, String reason);

    /**
     * 在庫ロック結果
     */
    record InventoryLockResult(
            UUID skuId,
            UUID inventoryLockId,
            Integer lockedQuantity,
            boolean success,
            String errorCode,
            String errorMessage
    ) {
    }
}
