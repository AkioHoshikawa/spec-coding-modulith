package com.example.modulith.poc.model.order.eventlistener;

import com.example.modulith.poc.event.inventory.ItemAllocate;
import com.example.modulith.poc.event.inventory.ItemAllocateComplete;
import com.example.modulith.poc.event.order.OrderCreate;
import com.example.modulith.poc.event.order.OrderCreateComplete;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class OrderListener {

    private final ApplicationEventPublisher publisher;

    @Autowired
    public OrderListener(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @ApplicationModuleListener
    public void onOrderCreate(OrderCreate event) {
        var itemAllocate = new ItemAllocate(event.getHeader(), event.getItemId(), event.getAmount());
        publisher.publishEvent(itemAllocate);
    }

    @ApplicationModuleListener
    public void onItemAllocateComplete(ItemAllocateComplete event) {
        var orderCreateComplete = new OrderCreateComplete(event.header(), "order1");
        publisher.publishEvent(orderCreateComplete);
    }
}
