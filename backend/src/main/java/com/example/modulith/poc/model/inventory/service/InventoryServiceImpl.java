package com.example.modulith.poc.model.inventory.service;

import com.example.modulith.poc.event.inventory.InventoryLock;
import com.example.modulith.poc.model.inventory.entity.InventoryEntity;
import com.example.modulith.poc.model.inventory.entity.InventoryTransactionEntity;
import com.example.modulith.poc.model.inventory.exception.InsufficientInventoryException;
import com.example.modulith.poc.model.inventory.repository.InventoryRepository;
import com.example.modulith.poc.model.inventory.repository.InventoryTransactionRepository;
import jakarta.persistence.OptimisticLockException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 在庫サービス実装
 */
@Service
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository transactionRepository;

    @Autowired
    public InventoryServiceImpl(InventoryRepository inventoryRepository,
                                InventoryTransactionRepository transactionRepository) {
        this.inventoryRepository = inventoryRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional
    public InventoryLockResult lockInventory(UUID orderId, UUID skuId, Integer quantity) {
        try {
            // 楽観ロック付きで在庫を取得
            InventoryEntity inventory = inventoryRepository.findBySkuIdWithLock(skuId)
                    .orElseThrow(() -> new IllegalArgumentException("在庫が見つかりません: SKU=" + skuId));

            // 在庫チェック
            if (inventory.getQuantity() < quantity) {
                throw new InsufficientInventoryException(skuId, quantity, inventory.getQuantity());
            }

            // 在庫を減少
            Integer quantityBefore = inventory.getQuantity();
            inventory.decreaseQuantity(quantity);
            inventoryRepository.save(inventory);
            Integer quantityAfter = inventory.getQuantity();

            // トランザクション記録
            UUID lockId = UUID.randomUUID();
            recordTransaction(skuId, "LOCK", -quantity, quantityBefore, quantityAfter, orderId,
                    "注文による在庫ロック: " + orderId);

            return new InventoryLockResult(skuId, lockId, quantity, true, null, null);

        } catch (InsufficientInventoryException e) {
            return new InventoryLockResult(skuId, null, null, false, "INSUFFICIENT_STOCK", e.getMessage());
        } catch (OptimisticLockException e) {
            return new InventoryLockResult(skuId, null, null, false, "OPTIMISTIC_LOCK_FAILURE",
                    "同時に注文が処理されたため、在庫を確保できませんでした");
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<InventoryLockResult> lockMultipleInventory(UUID orderId, List<InventoryLock.InventoryLockItem> items) {
        List<InventoryLockResult> results = new ArrayList<>();

        try {
            // すべてのSKUの在庫をロック
            for (InventoryLock.InventoryLockItem item : items) {
                InventoryLockResult result = lockInventory(orderId, item.skuId(), item.quantity());
                results.add(result);

                // 一つでも失敗したら全体を失敗とする
                if (!result.success()) {
                    throw new RuntimeException("在庫ロック失敗: " + result.errorMessage());
                }
            }

            return results;

        } catch (Exception e) {
            // ロールバックされるため、成功した分も戻る
            throw e;
        }
    }

    @Override
    @Transactional
    public void unlockInventory(UUID orderId, UUID skuId, Integer quantity) {
        InventoryEntity inventory = inventoryRepository.findById(skuId)
                .orElseThrow(() -> new IllegalArgumentException("在庫が見つかりません: SKU=" + skuId));

        Integer quantityBefore = inventory.getQuantity();
        inventory.increaseQuantity(quantity);
        inventoryRepository.save(inventory);
        Integer quantityAfter = inventory.getQuantity();

        recordTransaction(skuId, "UNLOCK", quantity, quantityBefore, quantityAfter, orderId,
                "注文キャンセルによる在庫解放: " + orderId);
    }

    @Override
    @Transactional
    public void recordTransaction(UUID skuId, String transactionType, Integer quantityChange,
                                  Integer quantityBefore, Integer quantityAfter, UUID referenceId, String reason) {
        InventoryTransactionEntity transaction = new InventoryTransactionEntity();
        transaction.setSkuId(skuId);
        transaction.setTransactionType(
                com.example.modulith.poc.model.inventory.entity.TransactionType.valueOf(transactionType)
        );
        transaction.setQuantityChange(quantityChange);
        transaction.setQuantityBefore(quantityBefore);
        transaction.setQuantityAfter(quantityAfter);
        transaction.setReferenceType("ORDER");
        transaction.setReferenceId(referenceId);
        transaction.setReason(reason);

        transactionRepository.save(transaction);
    }
}
