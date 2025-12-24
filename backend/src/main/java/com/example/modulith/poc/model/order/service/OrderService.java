package com.example.modulith.poc.model.order.service;

import com.example.modulith.poc.event.inventory.InventoryLockComplete;
import com.example.modulith.poc.event.inventory.InventoryLockFailed;
import com.example.modulith.poc.event.order.OrderCreate;
import com.example.modulith.poc.event.order.OrderCreateComplete;
import com.example.modulith.poc.model.order.entity.OrderEntity;
import com.example.modulith.poc.model.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 注文サービスインターフェース
 * <p>
 * 注文の作成・更新を管理する。
 */
public interface OrderService {

    /**
     * 注文を作成
     *
     * @param event 注文作成イベント
     * @return 作成された注文エンティティ
     */
    OrderEntity createOrder(OrderCreate event);


    /**
     * 在庫確保ができた時の処理。注文ステータスを更新し、在庫ロックIDを注文明細に設定する。
     * @param event InventoryLockCompleteイベント
     * @return OrderCreateComplete
     */
    OrderCreateComplete onInventoryLockComplete(InventoryLockComplete event);

    /**
     * 在庫確保に失敗した時の処理。注文をキャンセルする。
     * @param event InventoryLockFailedイベント
     * @return エラーフラグがtrueになっているOrderCreateComplete
     */
    OrderCreateComplete onInventoryLockFailed(InventoryLockFailed event);

    /**
     * 注文IDで検索
     *
     * @param orderId 注文ID
     * @return 注文エンティティ
     */
    OrderEntity findById(UUID orderId);
}
