package com.aws.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
@AllArgsConstructor
public class AwsAccountResponseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID userId;
    private String roleArn;
    private boolean active;
}