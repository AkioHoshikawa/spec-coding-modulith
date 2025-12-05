package com.example.modulith.poc.model.inventory.eventlistener;

import com.example.modulith.poc.event.inventory.ItemAllocate;
import com.example.modulith.poc.event.inventory.ItemAllocateComplete;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class InventoryListener {
    private final ApplicationEventPublisher publisher;

    @Autowired
    public InventoryListener(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @ApplicationModuleListener
    public void onItemAllocate(ItemAllocate event) {
        var itemAllocateComplete = new ItemAllocateComplete(event.header(), "alloc1");
        publisher.publishEvent(itemAllocateComplete);
    }
}
