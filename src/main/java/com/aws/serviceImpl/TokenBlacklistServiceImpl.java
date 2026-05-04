package com.aws.serviceImpl;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class TokenBlacklistServiceImpl {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String PREFIX = "blacklist:";

    public void blacklistToken(String token, long expiryTimeInMillis) {
        redisTemplate.opsForValue().set(
                PREFIX + token,
                "true",
                expiryTimeInMillis,
                TimeUnit.MILLISECONDS
        );
    }

    public boolean isBlacklisted(String token) {
        return redisTemplate.hasKey(PREFIX + token);
    }
}