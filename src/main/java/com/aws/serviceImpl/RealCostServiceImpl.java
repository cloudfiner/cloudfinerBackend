package com.aws.serviceImpl;

import com.aws.dto.*;
import com.aws.entity.AwsAccount;
import com.aws.entity.User;
import com.aws.repository.AwsAccountRepository;
import com.aws.repository.UserBudgetRepository;
import com.aws.service.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.costexplorer.*;
import software.amazon.awssdk.services.costexplorer.model.*;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.*;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealCostServiceImpl implements RealCostService {

    private final AwsAccountRepository awsAccountRepository;
    private final CurrentUserService currentUserService;
    private final CurrencyService currencyService;
    private final AIInsightService aiInsightService;
    private final UserBudgetRepository userBudgetRepository;

    private static final String METRIC = "UnblendedCost";
    private static final String DIMENSION = "SERVICE";
    private static final String SESSION_NAME = "cost-session";

    // ========================= CONTROLLER =========================
    @Override
    @Cacheable(value = "realCost", key = "#currency + '_' + #days")
    public CostResponseDto getRealCost(String currency, int days) {

        User user = currentUserService.getCurrentUser();

        if (user == null) {
            throw new RuntimeException("Unauthorized");
        }

        return processUserCost(user, currency, days);
    }

    // ========================= SCHEDULER =========================
    @Override
    public CostResponseDto getCostForUser(User user, String currency, int days) {

        try {
            AwsAccount account = awsAccountRepository.findByUserId(user.getId()).orElse(null);

            if (account == null || !account.isActive()) {
                return null;
            }

            return processUserCost(user, currency, days);

        } catch (Exception e) {
            log.error("Scheduler error for user {}", user.getId(), e);
            return null;
        }
    }

    // ========================= CORE =========================
    private CostResponseDto processUserCost(User user, String currency, int days) {

        AwsAccount account = awsAccountRepository.findByUserId(user.getId()).orElse(null);

        if (account == null || !account.isActive()) {
            return emptyResponse(user);
        }

        try {
            AwsSessionCredentials creds = assumeRole(account);

            List<DailyCostDto> daily;

            try (CostExplorerClient client = CostExplorerClient.builder()
                    .region(Region.US_EAST_1)
                    .credentialsProvider(StaticCredentialsProvider.create(creds))
                    .build()) {

                daily = fetchCost(client, currency, days);
            }

            if (daily.isEmpty()) {
                return emptyResponse(user);
            }

            return buildResponse(daily, user, currency);

        } catch (Exception e) {
            log.error("AWS fetch failed for user {}", user.getId(), e);
            return emptyResponse(user);
        }
    }

    // ========================= STS =========================
    private AwsSessionCredentials assumeRole(AwsAccount account) {

        try (StsClient sts = StsClient.create()) {

            AssumeRoleResponse res = sts.assumeRole(
                    AssumeRoleRequest.builder()
                            .roleArn(account.getRoleArn())
                            .externalId(account.getExternalId())
                            .roleSessionName(SESSION_NAME)
                            .build()
            );

            Credentials c = res.credentials();

            return AwsSessionCredentials.create(
                    c.accessKeyId(),
                    c.secretAccessKey(),
                    c.sessionToken()
            );

        } catch (Exception e) {
            log.error("STS failed", e);
            throw new RuntimeException("AWS authentication failed");
        }
    }

    // ========================= FETCH =========================
    private List<DailyCostDto> fetchCost(CostExplorerClient client, String currency, int days) {

        LocalDate end = LocalDate.now(ZoneOffset.UTC).plusDays(1);
        LocalDate start = end.minusDays(days);

        double rate = getRate(currency);

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

        GetCostAndUsageResponse response = client.getCostAndUsage(request);

        List<DailyCostDto> list = new ArrayList<>();

        for (ResultByTime r : response.resultsByTime()) {

            Map<String, Double> merged = new HashMap<>();

            for (Group g : r.groups()) {

                MetricValue mv = g.metrics().get(METRIC);
                if (mv == null || mv.amount() == null) continue;

                double cost = Double.parseDouble(mv.amount()) * rate;

                String service = normalize(g.keys().get(0));

                merged.put(service, merged.getOrDefault(service, 0.0) + cost);
            }

            List<ServiceCostDto> services = new ArrayList<>();
            for (Map.Entry<String, Double> e : merged.entrySet()) {
                services.add(new ServiceCostDto(e.getKey(), round(e.getValue())));
            }

            list.add(new DailyCostDto(r.timePeriod().start(), services));
        }

        return list;
    }

    // ========================= BUILD RESPONSE =========================
    private CostResponseDto buildResponse(List<DailyCostDto> list, User user, String currency) {

        double total = list.stream()
                .flatMap(d -> d.getServices().stream())
                .mapToDouble(ServiceCostDto::getCost)
                .sum();

        LocalDate todayDate = LocalDate.now(ZoneOffset.UTC);
        LocalDate yesterdayDate = todayDate.minusDays(1);

        double today = getCostForDate(list, todayDate.toString());
        double yesterday = getCostForDate(list, yesterdayDate.toString());

        double change = yesterday == 0 ? 0 : ((today - yesterday) / yesterday) * 100;

        ServiceCostDto topService = findTopService(list);

        List<InsightDto> insights;
        try {
            insights = aiInsightService.generateInsights(user.getId().toString(), list, change);
        } catch (Exception e) {
            insights = new ArrayList<>();
        }

        double budget = userBudgetRepository.findByEmail(user.getEmail())
                .map(b -> b.getMonthlyBudget().doubleValue())
                .orElse(0.0);

        CostResponseDto res = new CostResponseDto();
        res.setDailyData(list);
        res.setTotalCost(round(total));
        res.setYesterdayCost(round(yesterday));
        res.setPercentageChange(round(change));
        res.setTopService(topService);
        res.setInsights(insights);
        res.setPotentialSavings(round(total * 0.1));
        res.setMonthlyBudget(budget);

        return res;
    }

    // ========================= HELPERS =========================
    private double getRate(String currency) {
        try {
            double rate = currencyService.getRate(currency);
            return rate <= 0 ? 1.0 : rate;
        } catch (Exception e) {
            return 1.0;
        }
    }

    private double getCostForDate(List<DailyCostDto> list, String date) {
        return list.stream()
                .filter(d -> date.equals(d.getDate()))
                .flatMap(d -> d.getServices().stream())
                .mapToDouble(ServiceCostDto::getCost)
                .sum();
    }

    private ServiceCostDto findTopService(List<DailyCostDto> list) {

        Map<String, Double> map = new HashMap<>();

        for (DailyCostDto d : list) {
            for (ServiceCostDto s : d.getServices()) {
                map.put(s.getService(), map.getOrDefault(s.getService(), 0.0) + s.getCost());
            }
        }

        return map.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> new ServiceCostDto(e.getKey(), round(e.getValue())))
                .orElse(null);
    }

    private String normalize(String service) {
        return service == null ? "UNKNOWN" : service.trim().toUpperCase();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private CostResponseDto emptyResponse(User user) {

        double budget = userBudgetRepository.findByEmail(user.getEmail())
                .map(b -> b.getMonthlyBudget().doubleValue())
                .orElse(0.0);

        CostResponseDto res = new CostResponseDto();
        res.setDailyData(Collections.emptyList());
        res.setTotalCost(0.0);
        res.setYesterdayCost(0.0);
        res.setPercentageChange(0.0);
        res.setInsights(Collections.emptyList());
        res.setPotentialSavings(0.0);
        res.setMonthlyBudget(budget);

        return res;
    }
}