package com.aws.service;



import java.util.UUID;

import com.aws.entity.User;

public interface NotificationService {

	public void sendToUser(String email, String message, String priority);
    void markAsRead(User user, UUID id);

    void markAllRead(User user);
}