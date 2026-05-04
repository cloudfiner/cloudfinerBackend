package com.aws.serviceImpl;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.aws.service.MessageService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class MessageServiceImpl implements MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageServiceImpl.class);

    @Value("${telegram.bot.token}")
    private String token;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public void sendAlert(String message, String chatId) {

        if (chatId == null || chatId.isBlank()) {
            log.warn("Telegram chatId missing. Skipping alert.");
            return;
        }

        try {
            String url = "https://api.telegram.org/bot" + token + "/sendMessage";

            Map<String, String> body = new HashMap<>();
            body.put("chat_id", chatId);
            body.put("text", message);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Telegram alert sent successfully");
            } else {
                log.error("Telegram failed: {}", response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Telegram error: {}", e.getMessage());
        }
    }
}