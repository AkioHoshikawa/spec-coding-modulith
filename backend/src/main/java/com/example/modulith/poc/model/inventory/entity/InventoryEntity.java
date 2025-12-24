package com.example.modulith.poc.model.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 在庫エンティティ
 * <p>
 * SKU単位の在庫数量を管理する。
 * 楽観ロック（@Version）により同時実行制御を実現する。
 */
@Entity
@Table(name = "inventory")
public class InventoryEntity {
    @Id
    @Column(name = "sku_id")
    private UUID skuId;

    @Column(nullable = false)
    private Integer quantity = 0;

    @Version
    @Column(nullable = false)
    private Integer version = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    // Getters and Setters
    public UUID getSkuId() {
        return skuId;
    }

    public void setSkuId(UUID skuId) {
        this.skuId = skuId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * 在庫を減少させる
     *
     * @param amount 減少量
     * @throws IllegalArgumentException 在庫が不足している場合
     */
    public void decreaseQuantity(Integer amount) {
        if (this.quantity < amount) {
            throw new IllegalArgumentException(
                    String.format("在庫不足: SKU=%s, 要求数=%d, 利用可能数=%d",
                            skuId, amount, quantity)
            );
        }
        this.quantity -= amount;
    }

    /**
     * 在庫を増加させる
     *
     * @param amount 増加量
     */
    public void increaseQuantity(Integer amount) {
        this.quantity += amount;
    }
}
