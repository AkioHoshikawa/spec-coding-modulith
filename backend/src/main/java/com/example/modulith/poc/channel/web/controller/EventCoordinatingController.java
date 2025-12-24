package com.example.modulith.poc.channel.web.controller;

import com.example.modulith.poc.channel.web.exception.EventErrorException;
import com.example.modulith.poc.channel.web.service.EventCoordinatingService;
import com.example.modulith.poc.core.event.EventBase;
import reactor.core.publisher.Sinks;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EventCoordinatingController {
    private final EventCoordinatingService eventCoordinatingService;
    private final Map<String, Sinks.One<? extends EventBase>> responseMap = Collections.synchronizedMap(new HashMap<>());

    protected EventCoordinatingController(EventCoordinatingService eventCoordinatingService) {
        this.eventCoordinatingService = eventCoordinatingService;
    }

    protected <T extends EventBase> Sinks.One<T> publishEvent(EventBase event) {
        Sinks.One<T> sink = Sinks.one();
        responseMap.put(event.getHeader().getTxId(), sink);
        eventCoordinatingService.publishEvent(event);
        return sink;
    }

    protected <T extends EventBase> void emitResponse(T event) {
        String txId = event.getHeader().getTxId();
        Sinks.One<T> sink = (Sinks.One<T>) responseMap.get(txId);
        if (sink != null) {
            if (event.getHeader().isError()) {
                sink.tryEmitError(new EventErrorException(event.getHeader()));
            } else {
                sink.tryEmitValue(event);
            }
            responseMap.remove(txId);
        } else {
            throw new RuntimeException("failed to find corresponding response stream with: " + txId);
        }
    }

    protected <T extends EventBase> T validateEvent(T event) {
        if (event.getHeader().isError()) {
            throw new EventErrorException(event.getHeader());
        }
        return event;
    }
}
