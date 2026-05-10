package com.aws.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class BudgetResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID id;

    private String email;

    private BigDecimal monthlyBudget;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String status;
    private String message;
}