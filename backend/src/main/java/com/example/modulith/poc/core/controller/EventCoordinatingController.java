package com.example.modulith.poc.core.controller;

import com.example.modulith.poc.core.event.EventBase;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Sinks;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EventCoordinatingController {
    private final ApplicationEventPublisher events;
    private final Map<String, Sinks.One<? extends EventBase>> responseMap = Collections.synchronizedMap(new HashMap<>());

    protected EventCoordinatingController(ApplicationEventPublisher events) {
        this.events = events;
    }

    protected <T extends EventBase> Sinks.One<T> sendEvent(EventBase event) {
        Sinks.One<T> sink = Sinks.one();
        responseMap.put(event.getHeader().getTxId(), sink);
        events.publishEvent(event);
        return sink;
    }

    protected <T extends EventBase> void emitResponse(T event) {
        Sinks.One<T> sink = (Sinks.One<T>) responseMap.get(event.getHeader().getTxId());
        if (sink != null) {
            sink.tryEmitValue(event);
            responseMap.remove(event.getHeader().getTxId());
        } else {
            // TODO: Error handle
        }
    }
}
