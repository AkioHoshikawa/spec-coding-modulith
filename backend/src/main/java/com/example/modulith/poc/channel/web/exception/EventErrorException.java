package com.example.modulith.poc.channel.web.exception;

import com.example.modulith.poc.core.event.EventHeader;

/**
 * Eventの結果がエラーだった場合にThrowする例外
 */
public class EventErrorException extends RuntimeException {
    private final EventHeader eventHeader;

    public EventErrorException(EventHeader eventHeader) {
        this.eventHeader = eventHeader;
    }

    public EventErrorException(String message, EventHeader eventHeader) {
        super(message);
        this.eventHeader = eventHeader;
    }

    public EventErrorException(String message, Throwable cause, EventHeader eventHeader) {
        super(message, cause);
        this.eventHeader = eventHeader;
    }

    public EventErrorException(Throwable cause, EventHeader eventHeader) {
        super(cause);
        this.eventHeader = eventHeader;
    }

    public EventErrorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, EventHeader eventHeader) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.eventHeader = eventHeader;
    }
}
