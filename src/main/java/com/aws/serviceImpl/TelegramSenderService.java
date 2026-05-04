package com.aws.serviceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.aws.entity.User;
import com.aws.repository.TelegramLinkRepository;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class TelegramSenderService {

    @Autowired
    private TelegramLinkRepository repo;

    // ✅ ONLY ONE TOKEN (from application.yml)
    @Value("${telegram.bot.token}")
    private String BOT_TOKEN;
    
    

    public void send(User user, String message) {

        System.out.println("🔥 SEND METHOD CALLED");

        repo.findTopByUserAndUsedTrueOrderByIdDesc(user)
            .ifPresent(link -> {

                String chatId = link.getChatId();

                System.out.println("👉 CHAT ID: " + chatId);
                System.out.println("👉 BOT TOKEN: " + BOT_TOKEN);

                if (chatId == null) {
                    System.out.println("❌ chatId is null");
                    return;
                }

                String url = "https://api.telegram.org/bot"
                        + BOT_TOKEN
                        + "/sendMessage?chat_id="
                        + chatId
                        + "&text="
                        + URLEncoder.encode(message, StandardCharsets.UTF_8);

                System.out.println("👉 URL: " + url);

                try {
                    String response = new RestTemplate().getForObject(url, String.class);
                    System.out.println("✅ TELEGRAM RESPONSE: " + response);
                } catch (Exception e) {
                    System.out.println("❌ ERROR SENDING TELEGRAM MESSAGE");
                    e.printStackTrace();
                }
            });
        
    }
    
    

   
}