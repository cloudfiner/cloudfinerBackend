package com.aws.serviceImpl;

import com.aws.dto.AlertResponseDto;
import com.aws.entity.AlertConfig;
import com.aws.entity.User;
import com.aws.repository.AlertRepository;
import com.aws.repository.UserRepository;
import com.aws.service.AlertConfigService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertConfigServiceImpl implements AlertConfigService {

    private final AlertRepository alertRepository;
    private final UserRepository userRepository;
    private final CurrentUserServiceImpl currentUserService;

   
    // ================= GET ALERTS =================
    @Override
    public List<AlertResponseDto> getUserAlerts(String type) {

        // DEMO MODE
        if ("demo".equalsIgnoreCase(type)) {
            return getDemoAlerts();
        }

        // REAL USER
        User user =currentUserService.getCurrentUser();
        List<AlertConfig> alerts = alertRepository.findByUser(user);

        List<AlertResponseDto> list = new ArrayList<>();

        for (AlertConfig alert : alerts) {
            list.add(mapToDto(alert));
        }

        return list;
    }

    // ================= CREATE ALERT =================
    @Override
    public AlertResponseDto createAlert(double threshold) {

        if (threshold <= 0) {
            throw new IllegalArgumentException("Threshold must be greater than 0");
        }

        User user =currentUserService.getCurrentUser();

        AlertConfig alert = new AlertConfig();
        alert.setThreshold(threshold);
        alert.setActive(true);
        alert.setTriggered(false);
        alert.setUser(user);

        AlertConfig saved = alertRepository.save(alert);

        log.info("Alert created id={}, threshold={}", saved.getId(), threshold);

        return mapToDto(saved);
    }

    // ================= DELETE ALERT =================
    @Override
    public void deleteAlert(UUID alertId) {

        AlertConfig alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found"));

        if (!alert.getUser().getId().equals(currentUserService.getCurrentUser().getId())) {
            throw new RuntimeException("Unauthorized");
        }

        alertRepository.delete(alert);

        log.info("Alert deleted id={}", alertId);
    }

    // ================= TOGGLE ALERT =================
    @Override
    public AlertResponseDto toggleAlert(UUID alertId) {

        AlertConfig alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found"));

        if (!alert.getUser().getId().equals(currentUserService.getCurrentUser().getId())) {
            throw new RuntimeException("Unauthorized");
        }

        alert.setActive(!alert.isActive());

        AlertConfig updated = alertRepository.save(alert);

        return mapToDto(updated);
    }

    // ================= DEMO ALERTS =================
    private List<AlertResponseDto> getDemoAlerts() {

        List<AlertResponseDto> demoAlerts = new ArrayList<>();

        demoAlerts.add(new AlertResponseDto(
                UUID.randomUUID(),
                5000,
                true,
                false
        ));

        demoAlerts.add(new AlertResponseDto(
                UUID.randomUUID(),
                10000,
                false,
                false
        ));

        return demoAlerts;
    }

    // ================= MAPPER =================
    private AlertResponseDto mapToDto(AlertConfig alert) {
        return new AlertResponseDto(
                alert.getId(),
                alert.getThreshold(),
                alert.isActive(),
                alert.isTriggered()
        );
    }
}