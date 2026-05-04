package com.aws.serviceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.aws.dto.AlertEvent;
import com.aws.service.SmartNotificationService;

import io.github.resilience4j.retry.annotation.Retry;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertEventListener {

    private final SmartNotificationService notificationService;

    @Async("alertExecutor")
    @EventListener
    @Retry(name = "alertRetry", fallbackMethod = "fallback")
    public void handle(AlertEvent event) {

        if (event == null || event.getUser() == null) {
            log.warn("Invalid alert event received");
            return;
        }

        String userId = event.getUser().getId() != null
                ? event.getUser().getId().toString()
                : "unknown";

        try {

            log.info("Processing alert userId={} template={} value={}",
                    userId,
                    event.getTemplateKey(),
                    event.getValue());

            notificationService.send(
                    event.getUser(),
                    event.getTemplateKey(),
                    event.getValue()
            );

        } catch (Exception e) {

            log.error("Alert failed userId={} template={}",
                    userId,
                    event.getTemplateKey(),
                    e);

            throw e; // retry trigger
        }
    }

    // -------- FALLBACK --------
    public void fallback(AlertEvent event, Throwable t) {

        String userId = (event != null && event.getUser() != null)
                ? event.getUser().getId().toString()
                : "unknown";

        log.error("ALERT FAILED AFTER RETRY userId={} reason={}",
                userId,
                t.getMessage());

        // optional: save failed alert to DB / queue
    }
}