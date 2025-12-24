package com.example.modulith.poc.model.inventory.repository;

import com.example.modulith.poc.model.inventory.entity.InventoryTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * 在庫トランザクションリポジトリ
 * <p>
 * 在庫の増減履歴を管理する。
 */
@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransactionEntity, UUID> {

    /**
     * SKU IDで在庫トランザクション履歴を検索（作成日時降順）
     *
     * @param skuId SKU ID
     * @return 在庫トランザクションリスト
     */
    List<InventoryTransactionEntity> findBySkuIdOrderByCreatedAtDesc(UUID skuId);

    /**
     * 参照タイプと参照IDで在庫トランザクション履歴を検索
     *
     * @param referenceType 参照タイプ（ORDER, MANUALなど）
     * @param referenceId   参照ID
     * @return 在庫トランザクションリスト
     */
    List<InventoryTransactionEntity> findByReferenceTypeAndReferenceId(String referenceType, UUID referenceId);
}
