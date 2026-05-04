package com.aws.serviceImpl;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aws.dto.NotificationPayload;
import com.aws.entity.Notification;
import com.aws.entity.User;
import com.aws.repository.NotificationRepository;
import com.aws.repository.UserRepository;
import com.aws.service.NotificationService;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    // 🔔 SEND NOTIFICATION (WITH PRIORITY)
    @Override
    @Transactional
    public void sendToUser(String email, String message, String priority) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Notification notif = new Notification();
        notif.setMessage(message);
        notif.setUser(user);
        notif.setReadStatus(false);
        notif.setPriority(priority);

        notificationRepository.save(notif);

        NotificationPayload payload =
                NotificationPayload.newNotification(notif.getId(), message, priority);

        messagingTemplate.convertAndSendToUser(
                user.getEmail(),
                "/queue/alerts",
                payload
        );
    }

    // 📩 MARK SINGLE AS READ
    @Override
    @Transactional
    public void markAsRead(User user, UUID id) {

        Notification notification = notificationRepository.findById(id)
                .filter(n -> n.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.isReadStatus()) {
            notification.setReadStatus(true);
        }

        NotificationPayload payload = NotificationPayload.markRead(id);

        messagingTemplate.convertAndSendToUser(
                user.getEmail(),
                "/queue/alerts",
                payload
        );
    }

    // 📬 MARK ALL AS READ
    @Override
    @Transactional
    public void markAllRead(User user) {

        notificationRepository.markAllAsRead(user);

        NotificationPayload payload = NotificationPayload.markAllRead();

        messagingTemplate.convertAndSendToUser(
                user.getEmail(),
                "/queue/alerts",
                payload
        );
    }

    // 📊 UNREAD COUNT (IMPORTANT)
    public long getUnreadCount(User user) {
        return notificationRepository.countByUserAndReadStatusFalse(user);
    }
}