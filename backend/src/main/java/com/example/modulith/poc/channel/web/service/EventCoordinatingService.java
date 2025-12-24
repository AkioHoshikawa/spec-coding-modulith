package com.example.modulith.poc.channel.web.service;

import com.example.modulith.poc.core.event.EventBase;

public interface EventCoordinatingService {

    void publishEvent(EventBase event);
}
