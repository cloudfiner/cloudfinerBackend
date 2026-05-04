package com.aws.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "aws.insight")
@EnableCaching
public class InsightConfig {

    private double lowUsageThreshold;
    private double highPercentageThreshold;
    private double criticalPercentageThreshold;
    private double spikeThreshold;
}