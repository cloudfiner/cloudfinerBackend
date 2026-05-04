package com.aws.serviceImpl;

import com.aws.dto.*;
import com.aws.exception.AwsConnectionException;
import com.aws.service.AwsCostService;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.costexplorer.CostExplorerClient;
import software.amazon.awssdk.services.costexplorer.model.*;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AwsCostServiceImpl implements AwsCostService {

    private final CostExplorerClient costExplorerClient;
    private final CurrencyService currencyService;

    private static final String METRIC = "UnblendedCost";
    private static final String DIMENSION = "SERVICE";
    private static final String DEFAULT_CURRENCY = "USD";

    // ===========================
    // MAIN METHOD
    // ===========================
    @Override
    @Cacheable(value = "awsCost", key = "#currency")
    public CostResponseDto getAwsCost(String currency) {

        String finalCurrency = normalizeCurrency(currency);

        double rate = fetchCurrencyRate(finalCurrency);

        GetCostAndUsageResponse awsResponse = fetchAwsCost();

        return processResponse(awsResponse, finalCurrency, rate);
    }

    // ===========================
    // STEP 1: NORMALIZE INPUT
    // ===========================
    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return DEFAULT_CURRENCY;
        }
        return currency.toUpperCase();
    }

    // ===========================
    // STEP 2: GET RATE
    // ===========================
    private double fetchCurrencyRate(String currency) {
        try {
            double rate = currencyService.getRate(currency);
            return (rate <= 0) ? 1.0 : rate;
        } catch (Exception e) {
            log.warn("Currency service failed for {}, defaulting to 1.0", currency);
            return 1.0;
        }
    }

    // ===========================
    // STEP 3: AWS CALL
    // ===========================
    private GetCostAndUsageResponse fetchAwsCost() {

        LocalDate end = LocalDate.now();
        LocalDate start = end.withDayOfMonth(1);

        log.info("Fetching AWS cost from {} to {}", start, end);

        try {
            GetCostAndUsageRequest request = GetCostAndUsageRequest.builder()
                    .timePeriod(DateInterval.builder()
                            .start(start.toString())
                            .end(end.toString())
                            .build())
                    .granularity(Granularity.DAILY)
                    .metrics(METRIC)
                    .groupBy(GroupDefinition.builder()
                            .type(GroupDefinitionType.DIMENSION)
                            .key(DIMENSION)
                            .build())
                    .build();

            return costExplorerClient.getCostAndUsage(request);

        } catch (Exception e) {
            log.error("AWS API failed", e);
            throw new AwsConnectionException("Failed to fetch AWS cost data");
        }
    }

    // ===========================
    // STEP 4: PROCESS RESPONSE
    // ===========================
    private CostResponseDto processResponse(GetCostAndUsageResponse awsResponse,
                                            String currency,
                                            double rate) {

        if (awsResponse == null || awsResponse.resultsByTime() == null) {
            return emptyResponse();
        }

        List<DailyCostDto> dailyList = new ArrayList<>();
        double totalCost = 0.0;

        for (ResultByTime result : awsResponse.resultsByTime()) {

            if (result == null || result.groups() == null) continue;

            List<ServiceCostDto> services = new ArrayList<>();

            for (Group group : result.groups()) {

                Optional<ServiceCostDto> serviceCost =
                        extractServiceCost(group, currency, rate);

                serviceCost.ifPresent(sc -> {
                    services.add(sc);
                });
            }

            if (!services.isEmpty()) {
                dailyList.add(new DailyCostDto(result.timePeriod().start(), services));
                totalCost += calculateDayTotal(services);
            }
        }

        return buildResponse(dailyList, totalCost);
    }

    // ===========================
    // STEP 5: EXTRACT COST
    // ===========================
    private Optional<ServiceCostDto> extractServiceCost(Group group,
                                                       String currency,
                                                       double rate) {

        if (group == null || group.keys() == null || group.keys().isEmpty()) {
            return Optional.empty();
        }

        String service = group.keys().get(0);

        MetricValue metric = group.metrics().get(METRIC);
        if (metric == null || metric.amount() == null) {
            return Optional.empty();
        }

        try {
            double usd = Double.parseDouble(metric.amount());

            if (Double.isNaN(usd) || Double.isInfinite(usd)) {
                return Optional.empty();
            }

            double cost = DEFAULT_CURRENCY.equals(currency)
                    ? usd
                    : usd * rate;

            cost = round(cost);

            return Optional.of(new ServiceCostDto(service, cost));

        } catch (Exception e) {
            log.warn("Invalid cost for service {}", service);
            return Optional.empty();
        }
    }

    // ===========================
    // STEP 6: CALCULATE TOTAL
    // ===========================
    private double calculateDayTotal(List<ServiceCostDto> services) {
        return services.stream()
                .mapToDouble(ServiceCostDto::getCost)
                .sum();
    }

    // ===========================
    // STEP 7: BUILD RESPONSE
    // ===========================
    private CostResponseDto buildResponse(List<DailyCostDto> dailyList,
                                          double totalCost) {

        CostResponseDto response = new CostResponseDto();

        response.setDailyData(dailyList);
        response.setTotalCost(round(totalCost));
        response.setInsights(Collections.emptyList());
        response.setTopService(null);
        response.setPercentageChange(0.0);
        response.setPotentialSavings(0.0);

        return response;
    }

    // ===========================
    // EMPTY RESPONSE
    // ===========================
    private CostResponseDto emptyResponse() {
        return CostResponseDto.builder()
                .dailyData(new ArrayList<>())
                .totalCost(0.0)
                .topService(null)
                .yesterdayCost(0.0)
                .percentageChange(0.0)
                .potentialSavings(0.0)
                .monthlyBudget(0.0)
                .insights(new ArrayList<>())
                .build();
    }

    // ===========================
    // UTIL
    // ===========================
    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
    
   
}