package com.example.modulith.poc.model.order.entity;

import com.example.modulith.poc.core.event.EventHeader;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Embedded;
import jakarta.persistence.Column;
import java.time.OffsetDateTime;

@Entity
public class OrderEntity {
    @Id
    private String id;

    @Embedded
    private EventHeader header;

    @Column(name = "item_id")
    private String itemId;

    @Column(name = "amount")
    private Integer amount;

    @Column(name = "ordered_date")
    private OffsetDateTime orderedDate;

    public OrderEntity() {}

    public OrderEntity(EventHeader header, String itemId, Integer amount, OffsetDateTime orderedDate) {
        this.header = header;
        this.id = header.getTxId();
        this.itemId = itemId;
        this.amount = amount;
        this.orderedDate = orderedDate;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public EventHeader getHeader() { return header; }
    public void setHeader(EventHeader header) { this.header = header; }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public Integer getAmount() { return amount; }
    public void setAmount(Integer amount) { this.amount = amount; }

    public OffsetDateTime getOrderedDate() { return orderedDate; }
    public void setOrderedDate(OffsetDateTime orderedDate) { this.orderedDate = orderedDate; }
}
