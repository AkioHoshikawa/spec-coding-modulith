package com.example.modulith.poc.model.order.repository;

import com.example.modulith.poc.model.order.entity.OrderEntity;
import com.example.modulith.poc.model.order.entity.OrderLineEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * 注文明細リポジトリ
 * <p>
 * 注文の明細情報を管理する。
 */
@Repository
public interface OrderLineRepository extends JpaRepository<OrderLineEntity, UUID> {

    /**
     * 注文で注文明細を検索（明細番号順）
     *
     * @param order 注文エンティティ
     * @return 注文明細リスト
     */
    List<OrderLineEntity> findByOrderOrderByLineNumber(OrderEntity order);

    /**
     * 注文IDで注文明細を検索（明細番号順）
     *
     * @param orderId 注文ID
     * @return 注文明細リスト
     */
    List<OrderLineEntity> findByOrder_OrderIdOrderByLineNumber(UUID orderId);
}
