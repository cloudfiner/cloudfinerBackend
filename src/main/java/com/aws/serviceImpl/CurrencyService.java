package com.aws.serviceImpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import io.github.resilience4j.retry.annotation.Retry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class CurrencyService {

    private final RestTemplate restTemplate;

    private final Map<String, Double> cache = new ConcurrentHashMap<>();
    private final Map<String, Long> cacheTime = new ConcurrentHashMap<>();
    private final Map<String, Object> locks = new ConcurrentHashMap<>();

    @Value("${currency.cache.ttl:3600000}")
    private long cacheDuration;

    @Value("${currency.api.primary:https://open.er-api.com/v6/latest/USD}")
    private String primaryUrl;

    @Value("${currency.api.backup:https://api.exchangerate.host/latest?base=USD}")
    private String backupUrl;

    private static final double FALLBACK_RATE = 83.0;

    public CurrencyService() {
        this.restTemplate = createRestTemplate();
    }

    private RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(3000);
        return new RestTemplate(factory);
    }

    public double getRate(String currency) {

        if (currency == null || currency.isBlank()) {
            log.warn("Invalid currency input, using fallback");
            return FALLBACK_RATE;
        }

        currency = currency.toUpperCase();

        if ("USD".equals(currency)) {
            return 1.0;
        }

        long now = System.currentTimeMillis();

        Double cachedValue = cache.get(currency);
        Long lastTime = cacheTime.get(currency);

        if (cachedValue != null && lastTime != null && (now - lastTime < cacheDuration)) {
            return cachedValue;
        }

        Object lock = locks.computeIfAbsent(currency, k -> new Object());

        synchronized (lock) {

            cachedValue = cache.get(currency);
            lastTime = cacheTime.get(currency);

            if (cachedValue != null && lastTime != null && (now - lastTime < cacheDuration)) {
                return cachedValue;
            }

            try {
                double rate = fetchPrimary(currency);
                updateCache(currency, rate);
                return rate;

            } catch (Exception e) {
                log.warn("Primary API failed for {}: {}", currency, e.getMessage());

                try {
                    double rate = fetchBackup(currency);
                    updateCache(currency, rate);
                    return rate;

                } catch (Exception ex) {
                    log.error("Both APIs failed for {}", currency, ex);

                    return cache.getOrDefault(currency, FALLBACK_RATE);
                }
            }
        }
    }

    private void updateCache(String currency, double rate) {
        cache.put(currency, rate);
        cacheTime.put(currency, System.currentTimeMillis());
    }

    @Retry(name = "currencyRetry")
    private double fetchPrimary(String currency) {

        Map response = restTemplate.getForObject(primaryUrl, Map.class);

        if (response == null || !response.containsKey("rates")) {
            throw new RuntimeException("Invalid primary API response");
        }

        Map rates = (Map) response.get("rates");

        Object value = rates.get(currency);

        if (value == null) {
            throw new RuntimeException("Currency not found: " + currency);
        }

        return Double.parseDouble(value.toString());
    }

    @Retry(name = "currencyRetry")
    private double fetchBackup(String currency) {

        Map response = restTemplate.getForObject(backupUrl, Map.class);

        if (response == null || !response.containsKey("rates")) {
            throw new RuntimeException("Invalid backup API response");
        }

        Map rates = (Map) response.get("rates");

        Object value = rates.get(currency);

        if (value == null) {
            throw new RuntimeException("Currency not found: " + currency);
        }

        return Double.parseDouble(value.toString());
    }
}