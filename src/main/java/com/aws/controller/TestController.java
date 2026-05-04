package com.aws.controller;

import java.math.BigDecimal;
import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aws.entity.User;
import com.aws.repository.UserRepository;
import com.aws.service.MessageService;
import com.aws.service.SmartNotificationService;
import com.aws.serviceImpl.NotificationServiceImpl;
import com.aws.serviceImpl.RuleEngineService;
import com.aws.serviceImpl.TelegramSenderService;
import com.aws.template.AlertTemplate;

@RestController
@RequestMapping("/api/telegram")
public class TestController {

    @Autowired
    private MessageService messageService;
   
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    
    @Autowired
    private NotificationServiceImpl notificationService;
    

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private SmartNotificationService smartNotificationService;
    
    @Autowired
    private RuleEngineService ruleEngineService;
    
    
    @Autowired
    private TelegramSenderService telegramSenderService;


  
    
    @GetMapping("/test")
    public String test() {
        messageService.sendAlert("Hello test", "6500827199");
        return "sent";
    }
    
    @GetMapping("/send")
    public String sendTest() {

    	notificationService.sendToUser(
    		    "ankitbirgade@gmail.com",
    		    "🚨 Test new Alert from Backend",
    		    "INFO" // ✅ add this
    		);

        return "Sent";}
    
    
    @MessageMapping("/test")
    @SendToUser("/queue/alerts")
    public String handleMessage(String message) {

        System.out.println("FRONTEND SE AAYA: " + message);

        return "Echo: " + message;
    }
    
    

    @GetMapping("/smart-all")
    public String testAll() {

        User user = userRepository.findByEmail("ankitbirgade@gmail.com")
                .orElseThrow();

        smartNotificationService.send(user, "COST_ALERT", 8000);

        return "Sent";
    }
    
    
    @GetMapping("/alert")
    public String testAlert() {

        // ✅ Logged-in user nikalna
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        System.out.println("Sending alert to user: " + user.getEmail());

        // 🔥 rule trigger
        ruleEngineService.evaluateCost(user, new BigDecimal("99999"));

        // ✅ Telegram message bhejna
        telegramSenderService.send(user, "High cost alert triggered!");

        return "Triggered";
    }
}