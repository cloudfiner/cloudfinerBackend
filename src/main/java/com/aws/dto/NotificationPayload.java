package com.aws.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationPayload implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventType;
    private UUID id;
    private String message;
    private boolean readStatus;
    private String priority;

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