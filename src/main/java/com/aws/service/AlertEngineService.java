package com.aws.service;

import com.aws.entity.User;

public interface AlertEngineService {

    void processUserCost(User user, double cost);
}