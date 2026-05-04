package com.aws.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class BudgetResponse {

    private UUID id;

    private String email;

    private BigDecimal monthlyBudget;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 🔥 optional (API friendly)
    private String status;
    private String message;
}