package com.aws.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CostResponseDto {

    // ================= DATA =================
    private List<DailyCostDto> dailyData;
    private Double totalCost;

    // ================= ANALYTICS =================
    private ServiceCostDto topService;
    private Double yesterdayCost;
    private Double percentageChange;

    // ================= OPTIMIZATION =================
    private Double potentialSavings;
    private Double monthlyBudget;

    // ================= INSIGHTS =================
    private List<InsightDto> insights;
}