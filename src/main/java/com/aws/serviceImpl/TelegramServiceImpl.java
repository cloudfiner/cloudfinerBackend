package com.aws.serviceImpl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.aws.entity.TelegramLink;
import com.aws.entity.User;
import com.aws.repository.TelegramLinkRepository;
import com.aws.repository.UserRepository;
import com.aws.service.TelegramService;

@Service
@Transactional
public class TelegramServiceImpl implements TelegramService {

    @Autowired
    private TelegramLinkRepository repo;

    @Autowired
    private UserRepository userRepository;
    
    @Value("${telegram.bot.token}")
    private String BOT_TOKEN;
    
    private final RestTemplate restTemplate = new RestTemplate();


    @Override
    public String generateToken(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // cleanup old unused tokens
        repo.deleteByUserAndUsedFalse(user);

        String token = UUID.randomUUID().toString();

        TelegramLink link = new TelegramLink();
        link.setUser(user);
        link.setToken(token);
        link.setUsed(false);
        link.setExpiry(LocalDateTime.now().plusMinutes(10));

        repo.save(link);

        return token;
    }

    @Override
    public TelegramLink validateToken(String token) {

        token = token.trim(); // extra safety

        Optional<TelegramLink> optional = repo.findByTokenSafe(token);

        if (optional.isEmpty()) {
            System.out.println("TOKEN NOT FOUND ❌");
            return null;
        }

        TelegramLink link = optional.get();

        if (link.isUsed()) {
            System.out.println("TOKEN ALREADY USED ❌");
            return null;
        }

        if (link.getExpiry().isBefore(LocalDateTime.now())) {
            System.out.println("TOKEN EXPIRED ❌");
            return null;
        }

        return link;
    }

    @Override
    public void saveChatId(TelegramLink link, String chatId) {

        if (link == null) return;
        System.out.println("Saving chatId: " + chatId + " for user: " + link.getUser().getEmail());
        link.setChatId(chatId);
        link.setUsed(true);

        repo.save(link);
    }
    
    public void sendMessage(String chatId, String message) {

        String url = "https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage";

        Map<String, String> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", message);

        try {
            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, body, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("Telegram message sent successfully");
            } else {
                System.out.println("Failed to send message: " + response.getBody());
            }

        } catch (Exception e) {
            System.out.println("Error sending Telegram message: " + e.getMessage());
        }
    }
}