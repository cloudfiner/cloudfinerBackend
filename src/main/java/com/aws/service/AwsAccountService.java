package com.aws.service;

import java.util.UUID;

import com.aws.dto.AwsSetupResponseDto;
import com.aws.entity.AwsAccount;

public interface AwsAccountService {

    AwsAccount getByUserId(UUID userId);

    String connectAws(UUID userId, String roleArn);

    AwsSetupResponseDto generateSetup(UUID userId);

    String fallback(UUID userId, String roleArn, Throwable t);
}