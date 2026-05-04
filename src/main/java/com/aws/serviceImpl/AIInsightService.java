package com.aws.serviceImpl;

import com.aws.dto.*;
import com.aws.service.AlertService;
import com.aws.config.InsightConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIInsightService {

    private final InsightConfig config;
    private final AlertService alertService;

    // ✅ REAL USER INSIGHTS (CACHED)
    @Cacheable(value = "insights", key = "#userId")
    public List<InsightDto> generateInsights(String userId,
                                             List<DailyCostDto> data,
                                             double percentageChange) {

        return generateInternalInsights(userId, data, percentageChange, false);
    }

    // 🔥 DEMO INSIGHTS (NO CACHE, NO ALERT)
    public List<InsightDto> generateDemoInsights(List<DailyCostDto> data) {

        return generateInternalInsights("DEMO_USER", data, 0, true);
    }

    // ✅ COMMON LOGIC (DRY + CLEAN)
    private List<InsightDto> generateInternalInsights(String userId,
                                                      List<DailyCostDto> data,
                                                      double percentageChange,
                                                      boolean isDemo) {

        long startTime = System.currentTimeMillis();

        Set<InsightDto> insights = new LinkedHashSet<>();

        if (data == null || data.isEmpty()) {
            log.warn("No data available for user={}", userId);
            return Collections.singletonList(
                    new InsightDto("No data available", "INFO", "ALL")
            );
        }

        Map<String, Double> serviceTotal = new HashMap<>();
        double total = 0;

        for (DailyCostDto day : data) {

            if (day == null || day.getServices() == null) continue;

            for (ServiceCostDto s : day.getServices()) {

                if (s == null || s.getService() == null) continue;

                double cost = s.getCost();
                total += cost;

                String normalized = normalize(s.getService());

                serviceTotal.put(
                        normalized,
                        serviceTotal.getOrDefault(normalized, 0.0) + cost
                );
            }
        }

        // ✅ FREE TIER CASE
        if (Math.abs(total) < 0.01) {
            insights.add(new InsightDto(
                    "No significant AWS cost detected (Free Tier)",
                    "INFO",
                    "ALL"
            ));
            return new ArrayList<>(insights);
        }

        // ✅ TOP SERVICE
        String topService = null;
        double max = 0;

        for (Map.Entry<String, Double> entry : serviceTotal.entrySet()) {
            if (entry.getValue() > max) {
                max = entry.getValue();
                topService = entry.getKey();
            }
        }

        if (topService != null) {

            double roundedMax = round(max);

            insights.add(new InsightDto(
                    "Highest cost service: " + topService + " (₹" + roundedMax + ")",
                    "INFO",
                    topService
            ));

            // 🔥 ALERT ONLY FOR REAL USERS
            if (!isDemo) {

                if (max > total * config.getCriticalPercentageThreshold()) {

                    triggerOnce(insights, userId,
                            new InsightDto(
                                    topService + " is contributing more than 70% of total cost",
                                    "CRITICAL",
                                    topService
                            ));

                } else if (max > total * config.getHighPercentageThreshold()) {

                    insights.add(new InsightDto(
                            topService + " is contributing more than 50% of total cost",
                            "WARNING",
                            topService
                    ));
                }
            }
        }

        // ✅ LOW USAGE
        if (total < config.getLowUsageThreshold()) {
            insights.add(new InsightDto(
                    "Your AWS usage is minimal",
                    "INFO",
                    "ALL"
            ));
        }

        // 🔥 SPIKE ONLY FOR REAL USERS
        if (!isDemo && percentageChange > config.getSpikeThreshold()) {

            triggerOnce(insights, userId,
                    new InsightDto(
                            "Cost spike detected (" + round(percentageChange) + "% increase)",
                            "CRITICAL",
                            "ALL"
                    ));
        }

        // ✅ FALLBACK
        if (insights.isEmpty()) {
            insights.add(new InsightDto(
                    "Your AWS usage looks optimized",
                    "INFO",
                    "ALL"
            ));
        }

        long endTime = System.currentTimeMillis();

        log.info("Insights generated | user={} | totalCost={} | count={} | time={}ms",
                userId, total, insights.size(), (endTime - startTime));

        return new ArrayList<>(insights);
    }

    // 🔥 ALERT (ONLY REAL USERS)
    private void triggerOnce(Set<InsightDto> insights,
                             String userId,
                             InsightDto insight) {

        insights.add(insight);
        alertService.trigger(userId, insight);
    }

    // 🔧 SERVICE NORMALIZATION
    private String normalize(String service) {

        String s = service.toLowerCase();

        if (s.contains("ec2")) return "EC2";
        if (s.contains("s3")) return "S3";
        if (s.contains("rds")) return "RDS";
        if (s.contains("lambda")) return "LAMBDA";
        if (s.contains("dynamodb")) return "DYNAMODB";
        if (s.contains("cloudwatch")) return "CLOUDWATCH";
        if (s.contains("sns")) return "SNS";
        if (s.contains("sqs")) return "SQS";

        return service;
    }

    private double round(double val) {
        return Math.round(val * 100.0) / 100.0;
    }
}