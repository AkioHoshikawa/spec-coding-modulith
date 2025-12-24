package com.example.modulith.poc.channel.web.service;

import com.example.modulith.poc.core.event.EventBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventCoordinatingServiceImpl implements EventCoordinatingService {
    private final ApplicationEventPublisher publisher;

    @Autowired
    public EventCoordinatingServiceImpl(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    @Transactional
    public void publishEvent(EventBase event) {
        publisher.publishEvent(event);
    }
}
