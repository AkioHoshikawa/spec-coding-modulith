package com.example.modulith.poc.event.inventory;

import com.example.modulith.poc.core.event.EventHeader;

public record ItemAllocate (
        EventHeader header,
        String itemId,
        Integer amount
){
}
