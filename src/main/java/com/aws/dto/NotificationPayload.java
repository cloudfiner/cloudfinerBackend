package com.aws.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationPayload {

    private String eventType;   // NEW, READ, ALL_READ
    private UUID id;
    private String message;
    private boolean readStatus;
    private String priority;   // INFO, WARNING, ERROR

    public static NotificationPayload newNotification(UUID id, String message, String priority) {
        return new NotificationPayload("NEW", id, message, false, priority);
    }

    public static NotificationPayload markRead(UUID id) {
        return new NotificationPayload("READ", id, null, true, null);
    }

    public static NotificationPayload markAllRead() {
        return new NotificationPayload("ALL_READ", null, null, true, null);
    }
}