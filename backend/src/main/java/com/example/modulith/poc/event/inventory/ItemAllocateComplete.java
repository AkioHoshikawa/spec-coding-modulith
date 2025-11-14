package com.example.modulith.poc.event.inventory;

import com.example.modulith.poc.core.event.EventHeader;

public record ItemAllocateComplete(
        EventHeader header,
        String allocationId
){
}
