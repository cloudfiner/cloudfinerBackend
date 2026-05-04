package com.aws.serviceImpl;

import com.aws.exception.AwsConnectionException;

import lombok.RequiredArgsConstructor;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    // 🔥 configurable values
    private static final int MAX_REQUESTS = 3;
    private static final long WINDOW_SECONDS = 10;

    public void checkRateLimit(UUID userId) {

        String key = buildKey(userId);

        Long count = redisTemplate.opsForValue().increment(key);

        // null safety
        if (count == null) {
            throw new AwsConnectionException("Rate limit error. Try again.");
        }

        //  first request → set expiry
        if (count == 1) {
            redisTemplate.expire(key, Duration.ofSeconds(WINDOW_SECONDS));
        }

        //  limit exceeded
        if (count > MAX_REQUESTS) {
            throw new AwsConnectionException(
                    "Too many requests. Please wait " + WINDOW_SECONDS + " seconds."
            );
        }
    }

    // 🔥 clean key structure (important for scaling)
    private String buildKey(UUID userId) {
        return "aws:rate-limit:user:" + userId;
    }
}