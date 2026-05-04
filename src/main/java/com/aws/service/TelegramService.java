package com.aws.service;



import java.util.UUID;

import com.aws.entity.TelegramLink;

public interface TelegramService {

    String generateToken(UUID userId);

    TelegramLink validateToken(String token);   // ✅ changed

    void saveChatId(TelegramLink link, String chatId);  // ✅ changed
    
    void sendMessage(String chatId, String message);
}