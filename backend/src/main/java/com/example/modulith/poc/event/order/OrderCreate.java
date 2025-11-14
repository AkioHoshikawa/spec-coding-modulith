package com.example.modulith.poc.event.order;

import com.example.modulith.poc.core.event.EventBase;
import com.example.modulith.poc.core.event.EventHeader;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public final class OrderCreate extends EventBase {
    private final @NotBlank String itemId;
    private final @Min(1) Integer amount;
    private final @NotNull OffsetDateTime orderedDate;

    public OrderCreate(
            EventHeader header,
            String itemId,
            Integer amount,
            OffsetDateTime orderedDate
    ) {
        super(header);
        this.itemId = itemId;
        this.amount = amount;
        this.orderedDate = orderedDate;
    }

    public String getItemId() {
        return itemId;
    }

    public Integer getAmount() {
        return amount;
    }

    public OffsetDateTime getOrderedDate() {
        return orderedDate;
    }
}
