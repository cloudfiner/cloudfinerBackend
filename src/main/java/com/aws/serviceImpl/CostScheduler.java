//package com.aws.serviceImpl;
//
//import com.aws.dto.CostResponseDto;
//import com.aws.entity.User;
//import com.aws.repository.UserRepository;
//import com.aws.service.AlertEngineService;
//import com.aws.service.AwsCostService;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//
//import io.micrometer.core.instrument.MeterRegistry;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class CostScheduler {
//
//    private final UserRepository userRepository;
//    private final AwsCostService awsCostService;
//    private final AlertEngineService alertEngineService;
//    private final MeterRegistry meterRegistry;
//
//    private static final int PAGE_SIZE = 100;
//
//    // runs every 15 minutes
//    @Scheduled(fixedDelay = 900000)
//    public void checkCost() {
//
//        long startTime = System.currentTimeMillis();
//
//        int page = 0;
//        int totalProcessed = 0;
//        int totalErrors = 0;
//
//        log.info("CostScheduler started");
//
//        try {
//
//            while (true) {
//
//                Page<User> userPage =
//                        userRepository.findAll(PageRequest.of(page, PAGE_SIZE));
//
//                // stop when no more users
//                if (userPage.isEmpty()) {
//                    break;
//                }
//
//                for (User user : userPage.getContent()) {
//
//                    try {
//
//                        // fetch cost for user
//                        CostResponseDto res =
//                                awsCostService.getCostDataForUser(user, "USD", 1);
//
//                        // skip if AWS not connected or failed
//                        if (res == null) {
//                            continue;
//                        }
//
//                        double cost = res.getTotalCost();
//
//                        // skip invalid values
//                        if (cost <= 0) {
//                            continue;
//                        }
//
//                        // process alerts
//                        alertEngineService.processUserCost(user, cost);
//
//                        totalProcessed++;
//
//                    } catch (Exception ex) {
//
//                        totalErrors++;
//
//                        log.error("Cost processing failed for userId={}",
//                                user.getId(), ex);
//                    }
//                }
//
//                page++;
//            }
//
//        } catch (Exception e) {
//            log.error("CostScheduler fatal error", e);
//        }
//
//        long endTime = System.currentTimeMillis();
//
//        // metrics
//        meterRegistry.counter("costscheduler.processed").increment(totalProcessed);
//        meterRegistry.counter("costscheduler.errors").increment(totalErrors);
//
//        log.info("CostScheduler finished processed={} errors={} time={}ms",
//                totalProcessed, totalErrors, (endTime - startTime));
//    }
//}














package com.aws.serviceImpl;

import com.aws.dto.CostResponseDto;
import com.aws.entity.User;
import com.aws.repository.UserRepository;
import com.aws.service.AlertEngineService;
import com.aws.service.RealCostService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import io.micrometer.core.instrument.MeterRegistry;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class CostScheduler {

    private final UserRepository userRepository;
    private final RealCostService realCostService;
    private final AlertEngineService alertEngineService;
    private final MeterRegistry meterRegistry;

    private static final int PAGE_SIZE = 100;
    private static final int THREAD_POOL_SIZE = 10;
    private static final int TIMEOUT_SECONDS = 30;

    @Scheduled(fixedDelay = 900000) // 15 minutes
    public void checkCost() {

        long startTime = System.currentTimeMillis();

        AtomicInteger totalProcessed = new AtomicInteger(0);
        AtomicInteger totalErrors = new AtomicInteger(0);

        log.info("CostScheduler started");

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        try {
            int page = 0;

            while (true) {

                Page<User> userPage =
                        userRepository.findAll(PageRequest.of(page, PAGE_SIZE));

                if (userPage.isEmpty()) {
                    break;
                }

                for (User user : userPage.getContent()) {

                    executor.submit(() -> processUser(user, totalProcessed, totalErrors));
                }

                if (!userPage.hasNext()) {
                    break;
                }

                page++;
            }

            // wait for tasks
            executor.shutdown();

            boolean finished = executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!finished) {
                log.warn("Scheduler timeout reached, forcing shutdown");
                executor.shutdownNow();
            }

        } catch (Exception e) {
            log.error("CostScheduler fatal error", e);
        }

        long endTime = System.currentTimeMillis();

        meterRegistry.counter("costscheduler.processed")
                .increment(totalProcessed.get());

        meterRegistry.counter("costscheduler.errors")
                .increment(totalErrors.get());

        log.info("CostScheduler finished processed={} errors={} time={}ms",
                totalProcessed.get(),
                totalErrors.get(),
                (endTime - startTime));
    }

    // =========================
    // USER PROCESSING
    // =========================
    private void processUser(User user,
                             AtomicInteger totalProcessed,
                             AtomicInteger totalErrors) {

        if (user == null || user.getId() == null) {
            return;
        }

        try {

            CostResponseDto res =
                    realCostService.getCostForUser(user, "USD", 1);

            if (res == null) {
                return;
            }

            double cost = res.getTotalCost();

            if (cost <= 0) {
                return;
            }

            alertEngineService.processUserCost(user, cost);

            totalProcessed.incrementAndGet();

        } catch (Exception ex) {

            totalErrors.incrementAndGet();

            log.error("Cost processing failed for userId={}",
                    user.getId(), ex);
        }
    }
}