package com.aws.serviceImpl;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.aws.dto.AlertEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertEventPublisher {

    private final ApplicationEventPublisher publisher;

    public void publish(AlertEvent event) {

        if (event == null) {
            log.warn("Skipping null event");
            return;
        }

        log.debug("Publishing alert event for user={}", event.getUser());

        publisher.publishEvent(event);
    }
}