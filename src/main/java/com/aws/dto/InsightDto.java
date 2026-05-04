package com.aws.dto;



import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@AllArgsConstructor
@EqualsAndHashCode
public class InsightDto implements Serializable {

    private String message;
    private String type;      // INFO / WARNING / CRITICAL
    private String service;   // EC2, S3, etc.
}