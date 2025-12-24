package com.example.modulith.poc.model.inventory.repository;

import com.example.modulith.poc.model.inventory.entity.InventoryEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 在庫リポジトリ
 * <p>
 * SKU単位の在庫情報を管理する。
 * 楽観ロックを使用した同時実行制御を提供する。
 */
@Repository
public interface InventoryRepository extends JpaRepository<InventoryEntity, UUID> {

    /**
     * SKU IDで在庫を検索（楽観ロック付き）
     *
     * @param skuId SKU ID
     * @return 在庫エンティティ
     */
    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT i FROM InventoryEntity i WHERE i.skuId = :skuId")
    Optional<InventoryEntity> findBySkuIdWithLock(@Param("skuId") UUID skuId);

    /**
     * 複数のSKU IDで在庫を検索
     *
     * @param skuIds SKU IDリスト
     * @return 在庫エンティティリスト
     */
    List<InventoryEntity> findBySkuIdIn(List<UUID> skuIds);

    /**
     * 複数のSKU IDで在庫を検索（楽観ロック付き）
     *
     * @param skuIds SKU IDリスト
     * @return 在庫エンティティリスト
     */
    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT i FROM InventoryEntity i WHERE i.skuId IN :skuIds")
    List<InventoryEntity> findBySkuIdInWithLock(@Param("skuIds") List<UUID> skuIds);
}
