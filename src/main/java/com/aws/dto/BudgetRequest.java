package com.aws.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;



@Data
public class BudgetRequest {

    @NotNull(message = "Budget is required")
    @DecimalMin(value = "0.01", message = "Budget must be greater than 0")
    private BigDecimal monthlyBudget;
}