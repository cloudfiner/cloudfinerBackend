package com.aws.service;

import com.aws.dto.CostResponseDto;

public interface AwsCostService {
    CostResponseDto getAwsCost(String currency);
}