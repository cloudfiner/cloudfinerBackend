package com.aws.service;


import com.aws.dto.CostResponseDto;

public interface DemoCostService {

    CostResponseDto getDemoCost(String currency, int days);
}