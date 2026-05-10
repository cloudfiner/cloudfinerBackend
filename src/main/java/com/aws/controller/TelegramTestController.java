package com.aws.controller;

import com.aws.entity.User;
import com.aws.repository.UserRepository;
import com.aws.service.*;
import com.aws.serviceImpl.NotificationServiceImpl;
import com.aws.serviceImpl.RuleEngineService;
import com.aws.serviceImpl.TelegramSenderService;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/telegram")
public class TelegramTestController {

    private final MessageService messageService;
    private final NotificationServiceImpl notificationService;
    private final UserRepository userRepository;
    private final SmartNotificationService smartNotificationService;
    private final RuleEngineService ruleEngineService;
    private final TelegramSenderService telegramSenderService;
    private final RedisTemplate<String, Object> redisTemplate;

    public TelegramTestController(
            MessageService messageService,
            NotificationServiceImpl notificationService,
            UserRepository userRepository,
            SmartNotificationService smartNotificationService,
            RuleEngineService ruleEngineService,
            TelegramSenderService telegramSenderService,
            RedisTemplate<String, Object> redisTemplate) {
        
        this.messageService = messageService;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.smartNotificationService = smartNotificationService;
        this.ruleEngineService = ruleEngineService;
        this.telegramSenderService = telegramSenderService;
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/test")
    public String test() {
        messageService.sendAlert("Hello test", "6500827199");
        return "sent";
    }

    @GetMapping("/send")
    public String sendTest() {
        notificationService.sendToUser("ankitbirgade@gmail.com", "🚨 Test new Alert from Backend", "INFO");
        return "Sent";
    }

    @GetMapping("/smart-all")
    public String testAll() {
        User user = userRepository.findByEmail("ankitbirgade@gmail.com").orElseThrow();
        smartNotificationService.send(user, "COST_ALERT", 8000);
        return "Sent";
    }

    @GetMapping("/alert")
    public ResponseEntity<String> testAlert() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ruleEngineService.evaluateCost(user, new BigDecimal("99999"));
        telegramSenderService.send(user, "High cost alert triggered!");

        return ResponseEntity.ok("Triggered for user: " + user.getEmail());
    }

    @GetMapping("/redis")
    public String testRedis() {
        redisTemplate.opsForValue().set("name", "Ankit");
        Object value = redisTemplate.opsForValue().get("name");
        return "Redis Working: " + value;
    }
}