package com.aws.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AwsConnectRequestDto {

    @NotBlank(message = "Role ARN is required")
    private String roleArn;
}