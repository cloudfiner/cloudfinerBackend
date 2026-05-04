package com.aws.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class AwsAccountResponseDto {

    private UUID userId;
    private String roleArn;
    private boolean active;
}