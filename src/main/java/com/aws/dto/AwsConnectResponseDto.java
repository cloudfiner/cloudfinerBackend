package com.aws.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AwsConnectResponseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String message;
}