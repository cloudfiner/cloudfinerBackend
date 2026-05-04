package com.aws.controller;

import com.aws.dto.ApiResponse;
import com.aws.dto.CostResponseDto;
import com.aws.service.DemoCostService;
import com.aws.service.RealCostService;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/cost") // ✅ versioning added
@RequiredArgsConstructor
public class AwsCostController {

    private final DemoCostService demoService;
    private final RealCostService realService;

    // =========================
    // DEMO API
    // =========================
    @GetMapping("/demo")
    public ResponseEntity<ApiResponse<CostResponseDto>> getDemoCost(

            @RequestParam(defaultValue = "INR")
            @Pattern(regexp = "^(INR|USD|EUR)$", message = "Invalid currency")
            String currency,

            @RequestParam(defaultValue = "7")
            @Min(value = 1, message = "Days must be greater than 0")
            int days
    ) {

        log.info("DEMO API called | currency={} days={}", currency, days);

        CostResponseDto data = demoService.getDemoCost(currency, days);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Demo cost fetched successfully", data)
        );
    }

    // =========================
    // REAL API
    // =========================
    @GetMapping("/real")
    public ResponseEntity<ApiResponse<CostResponseDto>> getRealCost(

            @RequestParam(defaultValue = "INR")
            @Pattern(regexp = "^(INR|USD|EUR)$", message = "Invalid currency")
            String currency,

            @RequestParam(defaultValue = "7")
            @Min(value = 1, message = "Days must be greater than 0")
            int days
    ) {

        log.info("REAL API called | currency={} days={}", currency, days);

        CostResponseDto data = realService.getRealCost(currency, days);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Real cost fetched successfully", data)
        );
    }
}