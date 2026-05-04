package com.aws.controller;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.aws.entity.User;
import com.aws.repository.TelegramLinkRepository;
import com.aws.repository.UserRepository;
import com.aws.service.TelegramService;

@RestController
@RequestMapping("/api/telegram")
public class TelegramController {

    private static final Logger log = LoggerFactory.getLogger(TelegramController.class);

    @Autowired
    private TelegramService telegramService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TelegramLinkRepository telegramRepo;

    // ================= CONNECT =================
    @GetMapping("/connect")
    public ResponseEntity<Map<String, String>> connect() {

        User user = getLoggedInUser();

        // 🔥 CLEAN OLD UNUSED TOKENS
        telegramRepo.deleteByUserAndUsedFalse(user);

        String token = telegramService.generateToken(user.getId());

        String link = "https://t.me/cloudfiner_cost_analyser_bot?start=" + token;

        log.info("Telegram connect link generated for {}", user.getEmail());

        return ResponseEntity.ok(Map.of("link", link));
    }

    // ================= STATUS =================
    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> getStatus() {

        User user = getLoggedInUser();

        boolean connected = telegramRepo
                .existsByUserAndChatIdIsNotNullAndUsedTrue(user);

        return ResponseEntity.ok(Map.of("connected", connected));
    }

    // ================= DISCONNECT =================
    @PutMapping("/disconnect")
    public ResponseEntity<?> disconnect() {

        User user = getLoggedInUser();

        telegramRepo.findAllByUser(user).forEach(link -> {
            link.setChatId(null);
            link.setUsed(false); // 🔥 important
        });

        telegramRepo.saveAll(telegramRepo.findAllByUser(user));

        log.info("Telegram disconnected for {}", user.getEmail());

        return ResponseEntity.ok(Map.of("message", "Disconnected"));
    }

    // ================= AUTH USER =================
    private User getLoggedInUser() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }

        String email = auth.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    // ==================sending msg =========================
    @PostMapping("/send")
    public ResponseEntity<String> sendMessage(
            @RequestParam String chatId,
            @RequestParam String message
    ) {

    	telegramService.sendMessage(chatId, message);

        return ResponseEntity.ok("Message sent (check Telegram)");
    }
}