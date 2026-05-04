package com.aws.dto;



import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class AlertResponseDto {

	 private UUID id;
	    private double threshold;
	    private boolean active;
	    private boolean triggered;
   
   
}