package com.aws.dto;

import java.io.Serializable;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AwsConnectRequestDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Role ARN is required")
    private String roleArn;
}