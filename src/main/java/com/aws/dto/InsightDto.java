package com.aws.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@AllArgsConstructor
@EqualsAndHashCode
public class InsightDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String message;
    private String type;
    private String service;
}