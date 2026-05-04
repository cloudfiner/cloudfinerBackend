package com.aws.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AwsSetupResponseDto {

    private String accountId;
    private String externalId;
    private String policy;   //  ADD THIS

    private boolean success;
    private String instructions;
}