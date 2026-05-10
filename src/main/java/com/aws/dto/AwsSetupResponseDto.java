package com.aws.dto;

import java.io.Serializable;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AwsSetupResponseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String accountId;
    private String externalId;
    private String policy;

    private boolean success;
    private String instructions;
}