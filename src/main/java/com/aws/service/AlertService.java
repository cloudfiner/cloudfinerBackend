package com.aws.service;

import com.aws.dto.InsightDto;

public interface AlertService {

    void trigger(String userId, InsightDto insight);
}