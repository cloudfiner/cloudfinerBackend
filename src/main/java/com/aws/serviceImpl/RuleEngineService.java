package com.aws.serviceImpl;

import com.aws.dto.*;
import com.aws.entity.*;
import com.aws.repository.*;
import com.aws.service.RealCostService;
import com.aws.template.AlertType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuleEngineService {

    private final AlertRuleRepository ruleRepo;
    private final AlertExecutionRepository executionRepo;
    private final AlertEventPublisher eventPublisher;
    private final RealCostService realCostService;

    @Value("${alert.spike.percent:30}")
    private int spikePercent;

    @Value("${alert.spike.cooldown:10}")
    private int spikeCooldown;

    @Transactional
    public void evaluateCost(User user, BigDecimal cost) {

        if (user == null || cost == null) return;

        log.info("Evaluating cost for user {}", user.getId());

        List<AlertExecution> executions =
                executionRepo.findByUserId(user.getId());

        // -------- SPLIT EXECUTIONS --------
        Map<UUID, AlertExecution> executionMap =
                executions.stream()
                        .filter(ex -> ex.getRuleId() != null)
                        .collect(Collectors.toMap(
                                AlertExecution::getRuleId,
                                ex -> ex,
                                (existing, replacement) -> existing
                        ));

        // -------- SPIKE EXECUTION --------
        AlertExecution spikeExecution = executions.stream()
                .filter(ex -> ex.getRuleId() == null)
                .findFirst()
                .orElseGet(() -> createSpikeExecution(user));

        // -------- SPIKE DETECTION --------
        try {
            BigDecimal yesterday = getYesterdayCost(user);

            if (isSpike(cost, yesterday)) {

                if (isCooldownOver(spikeExecution, spikeCooldown)) {

                    eventPublisher.publish(
                            new AlertEvent(user, "COST_SPIKE", cost, "CRITICAL")
                    );

                    spikeExecution.setLastTriggeredAt(LocalDateTime.now());
                    executionRepo.save(spikeExecution);
                }
            }

        } catch (Exception e) {
            log.error("Spike detection failed", e);
        }

        // -------- RULES --------
        List<AlertRule> rules =
                ruleRepo.findByTypeAndUserAndActiveTrue(AlertType.COST, user);

        if (rules.isEmpty()) return;

        for (AlertRule rule : rules) {

            if (rule == null || rule.getThreshold() == null) continue;

            boolean state = match(rule, cost);

            AlertExecution ex =
                    executionMap.getOrDefault(rule.getId(), create(rule, user));

            if (!ex.isLastState() && state) {

                if (isCooldownOver(ex, rule.getCooldownMinutes())) {

                    ex.setTriggerCount(ex.getTriggerCount() + 1);

                    String priority = escalate(rule, ex);

                    eventPublisher.publish(
                            new AlertEvent(user, rule.getTemplateKey(), cost, priority)
                    );

                    ex.setLastTriggeredAt(LocalDateTime.now());
                }
            }

            if (ex.isLastState() && !state) {
                ex.setLastResolvedAt(LocalDateTime.now());
                ex.setTriggerCount(0);
            }

            ex.setLastState(state);
            executionRepo.save(ex);
        }
    }

    private BigDecimal getYesterdayCost(User user) {
        try {
            CostResponseDto res = realCostService.getCostForUser(user, "USD", 2);

            if (res == null || res.getDailyData() == null) return BigDecimal.ZERO;

            String yesterday = LocalDate.now(ZoneOffset.UTC)
                    .minusDays(1)
                    .toString();

            return res.getDailyData().stream()
                    .filter(d -> yesterday.equals(d.getDate()))
                    .flatMap(d -> d.getServices().stream())
                    .map(s -> BigDecimal.valueOf(s.getCost()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

        } catch (Exception e) {
            log.error("Yesterday cost fetch failed", e);
            return BigDecimal.ZERO;
        }
    }

    private boolean isCooldownOver(AlertExecution ex, int minutes) {
        if (minutes <= 0 || ex.getLastTriggeredAt() == null) return true;

        return ex.getLastTriggeredAt()
                .plusMinutes(minutes)
                .isBefore(LocalDateTime.now());
    }

    private boolean match(AlertRule r, BigDecimal value) {
        int cmp = value.compareTo(r.getThreshold());

        return switch (r.getCondition()) {
            case GREATER_THAN -> cmp > 0;
            case LESS_THAN -> cmp < 0;
            case EQUAL -> cmp == 0;
        };
    }

    private String escalate(AlertRule rule, AlertExecution ex) {
        if (ex.getTriggerCount() >= 5) return "CRITICAL";
        if (ex.getTriggerCount() >= 3) return "ERROR";
        return Optional.ofNullable(rule.getPriority()).orElse("INFO");
    }

    private boolean isSpike(BigDecimal today, BigDecimal yesterday) {

        if (yesterday.compareTo(BigDecimal.ZERO) == 0) return false;

        BigDecimal percent = today.subtract(yesterday)
                .divide(yesterday, 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        return percent.compareTo(BigDecimal.valueOf(spikePercent)) > 0;
    }

    private AlertExecution createSpikeExecution(User user) {
        AlertExecution ex = new AlertExecution();
        ex.setUserId(user.getId());
        ex.setLastState(false);
        ex.setTriggerCount(0);
        return ex;
    }

    private AlertExecution create(AlertRule rule, User user) {
        AlertExecution ex = new AlertExecution();
        ex.setRuleId(rule.getId());
        ex.setUserId(user.getId());
        ex.setLastState(false);
        ex.setTriggerCount(0);
        return ex;
    }
}