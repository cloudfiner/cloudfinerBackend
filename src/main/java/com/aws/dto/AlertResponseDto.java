package com.aws.dto;

import java.io.Serializable;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class AlertResponseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID id;
    private double threshold;
    private boolean active;
    private boolean triggered;
}