package com.aws.dto;

import java.io.Serializable;
import java.math.BigDecimal;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class BudgetRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "Budget is required")
    @DecimalMin(value = "0.01", message = "Budget must be greater than 0")
    private BigDecimal monthlyBudget;
}