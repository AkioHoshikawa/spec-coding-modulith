package com.example.modulith.poc.core.event;

public class EventBase {
    private final EventHeader header;

    public EventBase(EventHeader header) {
        this.header = header;
    }

    public EventHeader getHeader() {
        return header;
    }
}
