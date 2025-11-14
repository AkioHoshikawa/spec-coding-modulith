package com.example.modulith.poc.event.order;

import com.example.modulith.poc.core.event.EventBase;
import com.example.modulith.poc.core.event.EventHeader;

public final class OrderCreateComplete extends EventBase {
    private final String orderId;

    public OrderCreateComplete(
            EventHeader header,
            String orderId
    ) {
        super(header);
        this.orderId = orderId;
    }

    public String getOrderId() {
        return orderId;
    }
}
