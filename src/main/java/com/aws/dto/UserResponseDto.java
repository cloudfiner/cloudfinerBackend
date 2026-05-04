package com.aws.dto;


import java.util.List;
import java.util.UUID;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {
     
	private UUID id; 
    private String name;
    private String email;
   
    private List<String> roles;
    // ROLE_USER / ROLE_ADMIN
 

    // ACTIVE / INACTIVE
    private String status;
}