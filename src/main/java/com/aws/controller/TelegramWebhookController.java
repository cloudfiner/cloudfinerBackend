package com.aws.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.aws.entity.TelegramLink;
import com.aws.service.TelegramService;
import com.aws.serviceImpl.TelegramSenderService;

@RestController
@RequestMapping("/api/telegram")
public class TelegramWebhookController {

    private static final Logger log = LoggerFactory.getLogger(TelegramWebhookController.class);

    @Autowired
    private TelegramService telegramService;
    
    @Autowired
    private TelegramSenderService telegramSenderService;

    @PostMapping("/webhook")
    public ResponseEntity<?> handleWebhook(@RequestBody Map<String, Object> payload) {

        try {
            log.info("FULL PAYLOAD: {}", payload);

            // Extract message
            Map<String, Object> message = (Map<String, Object>) payload.get("message");
            if (message == null) {
                log.warn("NO MESSAGE FOUND");
                return ResponseEntity.ok().build();
            }

            String text = (String) message.get("text");

            Map<String, Object> chat = (Map<String, Object>) message.get("chat");
            if (chat == null) {
                log.warn("NO CHAT FOUND");
                return ResponseEntity.ok().build();
            }

            String chatId = String.valueOf(chat.get("id"));

            log.info("CHAT ID: {}", chatId);
            log.info("TEXT: {}", text);

            // Handle /start <token>
            if (text != null && text.startsWith("/start")) {

                String[] parts = text.trim().split(" ");

                if (parts.length < 2) {
                    log.warn("TOKEN NOT FOUND");
                    return ResponseEntity.ok().build();
                }

                // Clean token
                String token = parts[1].trim();
                token = token.replaceAll("[^a-zA-Z0-9\\-]", "");

                log.info("CLEAN TOKEN: {}", token);
                log.info("TOKEN LENGTH: {}", token.length());

                // Validate UUID length
                if (token.length() != 36) {
                    log.warn("INVALID TOKEN FORMAT");
                    return ResponseEntity.ok().build();
                }

                TelegramLink link = telegramService.validateToken(token);

                if (link == null) {
                    log.warn("INVALID OR EXPIRED TOKEN");
                    return ResponseEntity.ok().build();
                }

                telegramService.saveChatId(link, chatId);

                log.info("CHAT ID SAVED SUCCESSFULLY for userId={}", link.getUser().getId());
            }

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("ERROR IN TELEGRAM WEBHOOK", e);
            return ResponseEntity.ok().build();
        }
    }
    
    
   
}