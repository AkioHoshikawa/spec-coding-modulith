package com.example.modulith.poc.core.event;

import jakarta.persistence.Embeddable;

import java.time.OffsetDateTime;
import java.util.UUID;

@Embeddable
public class EventHeader {
    private boolean error;
    private String txId;
    private String userId;
    private OffsetDateTime createdDate;

    public EventHeader() {
    }

    public EventHeader(boolean error, String txId, String userId, OffsetDateTime createdDate) {
        this.error = error;
        this.txId = txId;
        this.userId = userId;
        this.createdDate = createdDate;
    }

    public EventHeader(String userId) {
        this(false, UUID.randomUUID().toString(), userId, OffsetDateTime.now());
    }

    public EventHeader(String txId, String userId) {
        this(false, txId, userId, OffsetDateTime.now());
    }

    public EventHeader(boolean error, String userId) {
        this(error, UUID.randomUUID().toString(), userId, OffsetDateTime.now());
    }

    public EventHeader(boolean error, String txId, String userId) {
        this(error, txId, userId, OffsetDateTime.now());
    }

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public String getTxId() {
        return txId;
    }

    public void setTxId(String txId) {
        this.txId = txId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public OffsetDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(OffsetDateTime createdDate) {
        this.createdDate = createdDate;
    }
}
