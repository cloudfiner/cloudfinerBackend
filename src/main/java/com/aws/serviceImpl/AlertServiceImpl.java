package com.aws.serviceImpl;

import com.aws.dto.InsightDto;
import com.aws.service.AlertService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertServiceImpl implements AlertService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void trigger(String userId, InsightDto insight) {

        // Redis Dedup Key
        String key = "alert:" + userId + ":" + insight.getMessage();

        Boolean exists = redisTemplate.hasKey(key);

        if (Boolean.TRUE.equals(exists)) {
            log.info("Duplicate alert skipped for user={} message={}", userId, insight.getMessage());
            return;
        }

        // Set TTL 
        redisTemplate.opsForValue().set(key, "sent", 5, TimeUnit.MINUTES);

        // ================= REAL ALERT =================

        log.warn("ALERT user={} message={}", userId, insight.getMessage());

       
    }
}




