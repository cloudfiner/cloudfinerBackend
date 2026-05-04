package com.aws.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.aws.entity.Notification;
import com.aws.entity.User;
import com.aws.repository.NotificationRepository;
import com.aws.repository.UserRepository;
import com.aws.security.CustomUserDetails;
import com.aws.service.NotificationService;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    // ================= SAFE USER =================
    private User getUserSafe() {
        try {
            Object principal = SecurityContextHolder
                    .getContext()
                    .getAuthentication()
                    .getPrincipal();

            if (principal instanceof CustomUserDetails userDetails) {
                return userRepository.findByEmail(userDetails.getUsername())
                        .orElse(null);
            }

        } catch (Exception e) {
            return null;
        }
        return null;
    }

    // ================= DEMO DATA =================
    private List<Notification> getDemoNotifications() {
        List<Notification> list = new ArrayList<>();

        Notification n1 = new Notification();
        n1.setId(UUID.randomUUID());
        n1.setMessage("High AWS cost detected");
        n1.setReadStatus(false);
        n1.setPriority("HIGH");
        n1.setCreatedAt(LocalDateTime.now());

        Notification n2 = new Notification();
        n2.setId(UUID.randomUUID());
        n2.setMessage("New optimization available");
        n2.setReadStatus(true);
        n2.setPriority("INFO");
        n2.setCreatedAt(LocalDateTime.now());

        list.add(n1);
        list.add(n2);

        return list;
    }

    // ================= GET ALL =================
    @GetMapping
    public ResponseEntity<List<Notification>> getAllNotifications(
            @RequestParam(required = false) String type
    ) {
        try {

            // demo mode
            if ("demo".equalsIgnoreCase(type)) {
                return ResponseEntity.ok(getDemoNotifications());
            }

            User user = getUserSafe();
            if (user == null) {
                return ResponseEntity.ok(new ArrayList<>());
            }

            return ResponseEntity.ok(
                    notificationRepository.findByUserOrderByCreatedAtDesc(user)
            );

        } catch (Exception e) {
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    // ================= MARK ONE =================
    @PutMapping("/{id}/read")
    public ResponseEntity<String> markAsRead(
            @PathVariable UUID id,
            @RequestParam(required = false) String type
    ) {
        try {

            if ("demo".equalsIgnoreCase(type)) {
                return ResponseEntity.ok("Demo mode");
            }

            User user = getUserSafe();
            if (user == null) {
                return ResponseEntity.badRequest().body("User not found");
            }

            notificationService.markAsRead(user, id);
            return ResponseEntity.ok("Notification marked as read");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error");
        }
    }

    // ================= MARK ALL =================
    @PutMapping("/read-all")
    public ResponseEntity<String> markAllAsRead(
            @RequestParam(required = false) String type
    ) {
        try {

            if ("demo".equalsIgnoreCase(type)) {
                return ResponseEntity.ok("Demo mode");
            }

            User user = getUserSafe();
            if (user == null) {
                return ResponseEntity.badRequest().body("User not found");
            }

            notificationService.markAllRead(user);
            return ResponseEntity.ok("All notifications marked as read");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error");
        }
    }

    // ================= UNREAD COUNT =================
    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(
            @RequestParam(required = false) String type
    ) {
        try {

            if ("demo".equalsIgnoreCase(type)) {
                return ResponseEntity.ok(1L);
            }

            User user = getUserSafe();
            if (user == null) {
                return ResponseEntity.ok(0L);
            }

            return ResponseEntity.ok(
                    notificationRepository.countByUserAndReadStatusFalse(user)
            );

        } catch (Exception e) {
            return ResponseEntity.ok(0L);
        }
    }
}