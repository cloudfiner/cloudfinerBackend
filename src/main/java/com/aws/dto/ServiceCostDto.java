package com.aws.dto;

import java.io.Serializable;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ServiceCostDto  implements Serializable{

    private String service;
    private Double cost;
}