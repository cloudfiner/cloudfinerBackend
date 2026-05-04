
package com.aws.serviceImpl;

import com.aws.dto.*;
import com.aws.service.DemoCostService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
public class DemoCostServiceImpl implements DemoCostService {

    // 🔒 Fixed seed → stable demo data
    private final Random random = new Random(42);

    // =========================
    // MAIN METHOD
    // =========================
    public CostResponseDto getDemoCost(String currency, int days) {

        log.info("Generating DEMO cost data");

        // ✅ validation
        if (days <= 0) days = 7;

        if (!currency.equalsIgnoreCase("USD") && !currency.equalsIgnoreCase("INR")) {
            throw new RuntimeException("Invalid currency. Only USD/INR supported");
        }

        double rate = currency.equalsIgnoreCase("USD") ? 1 : 83.0;

        List<DailyCostDto> dailyList = new ArrayList<>();
        Map<String, Double> serviceTotal = new HashMap<>();

        double total = 0;
        LocalDate today = LocalDate.now();

        for (int i = days - 1; i >= 0; i--) {

            LocalDate date = today.minusDays(i);

            double base = 100 + random.nextDouble() * 80;

            double ec2 = base * (0.45 + random.nextDouble() * 0.1) * rate;
            double s3  = base * (0.15 + random.nextDouble() * 0.1) * rate;
            double rds = base * (0.25 + random.nextDouble() * 0.1) * rate;

            double dayTotal = ec2 + s3 + rds;
            total += dayTotal;

            add(serviceTotal, "EC2", ec2);
            add(serviceTotal, "S3", s3);
            add(serviceTotal, "RDS", rds);

            dailyList.add(new DailyCostDto(
                    date.toString(),
                    Arrays.asList(
                            new ServiceCostDto("EC2", round(ec2)),
                            new ServiceCostDto("S3", round(s3)),
                            new ServiceCostDto("RDS", round(rds))
                    )
            ));
        }

        // 🔥 never empty
        if (dailyList.isEmpty()) {
            dailyList = List.of(
                    new DailyCostDto(
                            today.toString(),
                            List.of(
                                    new ServiceCostDto("EC2", 120 * rate),
                                    new ServiceCostDto("S3", 80 * rate)
                            )
                    )
            );
        }

        // =========================
        // BUILD RESPONSE
        // =========================
        CostResponseDto res = new CostResponseDto();

        res.setDailyData(dailyList);
        res.setTotalCost(round(total));
        res.setPercentageChange(calcChange(dailyList));
        res.setInsights(generateInsights(total));
        res.setMonthlyBudget(getDemoBudget());
        res.setPotentialSavings(round(total * 0.1));

        return res;
    }

    // =========================
    // HELPERS
    // =========================
    private void add(Map<String, Double> map, String key, double value) {
        map.put(key, map.getOrDefault(key, 0.0) + value);
    }

    private double calcChange(List<DailyCostDto> list) {
        if (list.size() < 2) return 0;

        double yesterday = sum(list.get(list.size() - 2));
        double today = sum(list.get(list.size() - 1));

        if (yesterday == 0) return 0;

        return ((today - yesterday) / yesterday) * 100;
    }

    private double sum(DailyCostDto day) {
        if (day == null || day.getServices() == null) return 0;

        return day.getServices()
                .stream()
                .mapToDouble(ServiceCostDto::getCost)
                .sum();
    }

    private List<InsightDto> generateInsights(double total) {

        List<InsightDto> insights = new ArrayList<>();

        insights.add(new InsightDto(
                "EC2 usage is highest contributor",
                "WARNING",
                "EC2"
        ));

        insights.add(new InsightDto(
                "S3 storage can be optimized using lifecycle rules",
                "INFO",
                "S3"
        ));

        insights.add(new InsightDto(
                "Consider reserved instances to reduce RDS cost",
                "INFO",
                "RDS"
        ));

        if (total > 50000) {
            insights.add(new InsightDto(
                    "High cost detected - review EC2 scaling",
                    "WARNING",
                    "EC2"
            ));
        }

        if (total < 5000) {
            insights.add(new InsightDto(
                    "Low usage detected - possibly free tier",
                    "INFO",
                    "ALL"
            ));
        }

        return insights;
    }

    private double getDemoBudget() {
        return 20000; // fixed demo budget
    }

    private double round(double val) {
        return Math.round(val * 100.0) / 100.0;
    }
}