package com.aws.service;



import com.aws.dto.CostResponseDto;
import com.aws.entity.User;

public interface RealCostService {
    CostResponseDto getRealCost(String currency, int days);
    CostResponseDto getCostForUser(User user, String currency, int days);
}