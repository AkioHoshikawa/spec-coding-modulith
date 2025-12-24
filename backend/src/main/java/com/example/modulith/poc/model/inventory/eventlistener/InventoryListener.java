package com.example.modulith.poc.model.inventory.eventlistener;

import com.example.modulith.poc.core.event.EventHeader;
import com.example.modulith.poc.event.inventory.InventoryLock;
import com.example.modulith.poc.event.inventory.InventoryLockComplete;
import com.example.modulith.poc.event.inventory.InventoryLockFailed;
import com.example.modulith.poc.model.inventory.service.InventoryService;
import jakarta.persistence.OptimisticLockException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 在庫イベントリスナー
 * <p>
 * 在庫ロック関連のドメインイベントを処理する。
 */
@Component
public class InventoryListener {

    private final ApplicationEventPublisher publisher;
    private final InventoryService inventoryService;

    @Autowired
    public InventoryListener(ApplicationEventPublisher publisher, InventoryService inventoryService) {
        this.publisher = publisher;
        this.inventoryService = inventoryService;
    }

    /**
     * 在庫ロックイベントを処理
     * <p>
     * 1. 複数SKUの在庫をロック
     * 2. 成功時: InventoryLockCompleteイベント発行
     * 3. 失敗時: InventoryLockFailedイベント発行
     */
    @ApplicationModuleListener
    public void onInventoryLock(InventoryLock event) {
        try {
            // 複数SKUの在庫ロック
            List<InventoryService.InventoryLockResult> results =
                    inventoryService.lockMultipleInventory(event.getOrderId(), event.getItems());

            // すべて成功の場合
            if (results.stream().allMatch(InventoryService.InventoryLockResult::success)) {
                List<InventoryLockComplete.InventoryLockResult> completeResults = results.stream()
                        .map(r -> new InventoryLockComplete.InventoryLockResult(
                                r.skuId(),
                                r.inventoryLockId(),
                                r.lockedQuantity()
                        ))
                        .collect(Collectors.toList());

                publisher.publishEvent(new InventoryLockComplete(
                        event.getHeader(),
                        event.getOrderId(),
                        completeResults
                ));
            } else {
                // 一部でも失敗の場合
                List<InventoryLockFailed.InventoryError> errors = results.stream()
                        .filter(r -> !r.success())
                        .map(r -> new InventoryLockFailed.InventoryError(
                                r.skuId(),
                                r.errorCode(),
                                r.errorMessage(),
                                event.getItems().stream()
                                        .filter(item -> item.skuId().equals(r.skuId()))
                                        .findFirst()
                                        .map(InventoryLock.InventoryLockItem::quantity)
                                        .orElse(0),
                                0
                        ))
                        .collect(Collectors.toList());

                EventHeader errorHeader = new EventHeader(true, event.getHeader().getTxId(), event.getHeader().getUserId());
                publisher.publishEvent(new InventoryLockFailed(errorHeader, event.getOrderId(), errors));
            }

        } catch (OptimisticLockException e) {
            // 楽観ロック競合
            List<InventoryLockFailed.InventoryError> errors = new ArrayList<>();
            errors.add(new InventoryLockFailed.InventoryError(
                    null,
                    "OPTIMISTIC_LOCK_FAILURE",
                    "同時に注文が処理されたため、在庫を確保できませんでした。再度お試しください。",
                    null,
                    null
            ));

            EventHeader errorHeader = new EventHeader(true, event.getHeader().getTxId(), event.getHeader().getUserId());
            publisher.publishEvent(new InventoryLockFailed(errorHeader, event.getOrderId(), errors));

        } catch (Exception e) {
            // その他のエラー
            List<InventoryLockFailed.InventoryError> errors = new ArrayList<>();
            errors.add(new InventoryLockFailed.InventoryError(
                    null,
                    "SYSTEM_ERROR",
                    "システムエラーが発生しました: " + e.getMessage(),
                    null,
                    null
            ));

            EventHeader errorHeader = new EventHeader(true, event.getHeader().getTxId(), event.getHeader().getUserId());
            publisher.publishEvent(new InventoryLockFailed(errorHeader, event.getOrderId(), errors));
        }
    }
}
