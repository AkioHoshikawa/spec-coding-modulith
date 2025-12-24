package com.example.modulith.poc.model.order.eventlistener;

import com.example.modulith.poc.core.event.EventHeader;
import com.example.modulith.poc.event.inventory.InventoryLock;
import com.example.modulith.poc.event.inventory.InventoryLockComplete;
import com.example.modulith.poc.event.inventory.InventoryLockFailed;
import com.example.modulith.poc.event.order.OrderCreate;
import com.example.modulith.poc.event.order.OrderCreateComplete;
import com.example.modulith.poc.model.order.entity.OrderEntity;
import com.example.modulith.poc.model.order.entity.OrderLineEntity;
import com.example.modulith.poc.model.order.entity.OrderStatus;
import com.example.modulith.poc.model.order.repository.OrderLineRepository;
import com.example.modulith.poc.model.order.repository.OrderRepository;
import com.example.modulith.poc.model.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 注文イベントリスナー
 * <p>
 * 注文関連のドメインイベントを処理する。
 */
@Component
public class OrderListener {

    private final ApplicationEventPublisher publisher;
    private final OrderService orderService;

    @Autowired
    public OrderListener(ApplicationEventPublisher publisher, OrderService orderService) {
        this.publisher = publisher;
        this.orderService = orderService;
    }

    /**
     * 注文作成イベントのListener
     */
    @ApplicationModuleListener
    public void onOrderCreate(OrderCreate event) {
        // 注文エンティティ作成
        OrderEntity order = orderService.createOrder(event);

        // 在庫ロックイベント発行
        List<InventoryLock.InventoryLockItem> lockItems = event.getItems().stream()
                .map(item -> new InventoryLock.InventoryLockItem(item.skuId(), item.quantity()))
                .collect(Collectors.toList());

        InventoryLock inventoryLock = new InventoryLock(event.getHeader(), order.getOrderId(), lockItems);
        publisher.publishEvent(inventoryLock);
    }

    /**
     * 在庫ロック完了イベントのListener
     */
    @ApplicationModuleListener
    public void onInventoryLockComplete(InventoryLockComplete event) {
        OrderCreateComplete completeEvent = orderService.onInventoryLockComplete(event);
        publisher.publishEvent(completeEvent);
    }

    /**
     * 在庫ロック失敗イベントのListener
     */
    @ApplicationModuleListener
    public void onInventoryLockFailed(InventoryLockFailed event) {
        OrderCreateComplete completeEvent = orderService.onInventoryLockFailed(event)
        publisher.publishEvent(completeEvent);
    }
}
