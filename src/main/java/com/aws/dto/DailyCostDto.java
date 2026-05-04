package com.aws.dto;



import lombok.*;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DailyCostDto  implements Serializable{

    private String date;
    private List<ServiceCostDto> services;
}