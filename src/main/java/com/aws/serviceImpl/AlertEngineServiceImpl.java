package com.aws.serviceImpl;

import com.aws.dto.InsightDto;
import com.aws.entity.AlertConfig;
import com.aws.entity.User;
import com.aws.repository.AlertRepository;
import com.aws.service.AlertEngineService;
import com.aws.service.AlertService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import io.micrometer.core.instrument.MeterRegistry;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertEngineServiceImpl implements AlertEngineService {

    private final AlertRepository alertRepository;
    private final AlertService alertService;
    private final MeterRegistry meterRegistry;

    private static final int PAGE_SIZE = 100;

    @Override
    public void processUserCost(User user, double currentCost) {

        if (user == null) {
            log.warn("User is null, skipping alert processing");
            return;
        }

        int page = 0;
        int processedCount = 0;

        Page<AlertConfig> alertPage;

        do {

            alertPage = alertRepository.findByUserAndActiveTrue(
                    user,
                    PageRequest.of(page, PAGE_SIZE)
            );

            if (alertPage.isEmpty()) break;

            List<AlertConfig> updates = new ArrayList<>();

            for (AlertConfig alert : alertPage.getContent()) {

            	if (alert == null || alert.getThreshold() == null) {
            	    continue;
            	}

                try {

                    boolean thresholdCrossed = currentCost > alert.getThreshold();

                    // RESET
                    if (!thresholdCrossed && alert.isTriggered()) {
                        alert.setTriggered(false);
                        updates.add(alert);
                        continue;
                    }

                    // TRIGGER
                    if (thresholdCrossed && !alert.isTriggered()) {

                        String message = buildMessage(alert, currentCost);

                        alertService.trigger(
                                user.getId().toString(),
                                new InsightDto(message, "CRITICAL", "COST")
                        );

                        alert.setTriggered(true);
                        updates.add(alert);

                        log.info("Alert triggered userId={} cost={}",
                                user.getId(), currentCost);
                    }

                    processedCount++;

                } catch (Exception e) {
                    log.error("Alert failed userId={}", user.getId(), e);
                }
            }

            if (!updates.isEmpty()) {
                alertRepository.saveAll(updates);
            }

            page++;

        } while (alertPage.hasNext());

        meterRegistry.counter("alerts.processed").increment(processedCount);
    }

    private String buildMessage(AlertConfig alert, double cost) {
        return "AWS cost exceeded threshold. Current=" + cost +
                " Threshold=" + alert.getThreshold();
    }
}