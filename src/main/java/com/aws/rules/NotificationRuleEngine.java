package com.aws.rules;

import org.springframework.stereotype.Component;

@Component
public class NotificationRuleEngine {

    public boolean shouldSendCostAlert(double cost) {
        return cost > 5000;
    }

    public boolean shouldSendLoginAlert(int failedAttempts) {
        return failedAttempts >= 3;
    }

    public boolean shouldSendServerAlert(boolean isDown) {
        return isDown;
    }
}