package com.aws.serviceImpl;

import com.aws.entity.User;
import com.aws.service.SmartNotificationService;
import com.aws.service.TemplateService;
import com.aws.util.PriorityDetector;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmartNotificationServiceImpl implements SmartNotificationService {

    private final NotificationServiceImpl notificationService;
    private final TemplateService templateService;
    private final TelegramSenderService telegramSenderService;

    @Override
    public void send(User user, String templateKey, Object... args) {

        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            log.error("Invalid user or email, cannot send notification");
            return;
        }

        String message = null;
        String priority = null;

        try {
            // Step 1: Build message from template
            try {
                message = templateService.buildMessage(templateKey, args);
            } catch (Exception e) {
                log.warn("Template not found for key: {}, using fallback", templateKey);
                message = "Notification: " + templateKey;
            }

            if (message == null || message.isBlank()) {
                message = "Notification: " + templateKey;
            }

            // Step 2: Get priority
            priority = templateService.getPriority(templateKey);

            if (priority == null || priority.isBlank()) {
                priority = PriorityDetector.detect(message);
            }

            // Step 3: Send WebSocket notification
            notificationService.sendToUser(
                    user.getEmail(),
                    message,
                    priority
            );

        } catch (Exception e) {
            log.error("WebSocket notification failed for user: {}", user.getEmail(), e);
        }

        // Step 4: Send Telegram (separate try so failure does not affect others)
        try {
            telegramSenderService.send(user, message);
        } catch (Exception e) {
            log.error("Telegram notification failed for user: {}", user.getEmail(), e);
        }

        log.info("Notification processed for user: {} | priority: {}", 
                user.getEmail(), priority);
    }
}