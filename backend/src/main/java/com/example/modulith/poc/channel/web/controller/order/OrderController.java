package com.example.modulith.poc.channel.web.controller.order;

import com.example.modulith.poc.core.controller.EventCoordinatingController;
import com.example.modulith.poc.core.event.EventBase;
import com.example.modulith.poc.core.event.EventHeader;
import com.example.modulith.poc.event.order.OrderCreate;
import com.example.modulith.poc.event.order.OrderCreateComplete;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("order")
public class OrderController extends EventCoordinatingController {

    @Autowired
    public OrderController(ApplicationEventPublisher events) {
        super(events);
    }

    @PostMapping("create")
    @Transactional
    public Mono<OrderCreateComplete> order() {
        var orderCreate = new OrderCreate(new EventHeader("user1"), "item1", 1, OffsetDateTime.now());
        return super.<OrderCreateComplete>sendEvent(orderCreate).asMono();
    }

    @EventListener({OrderCreateComplete.class})
    public void onEventComplete(EventBase event) {
        emitResponse(event);
    }
}
