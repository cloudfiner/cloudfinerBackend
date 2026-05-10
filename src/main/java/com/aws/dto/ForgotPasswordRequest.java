package com.aws.dto;

import java.io.Serializable;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPasswordRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Email(message = "Invalid email")
    @NotBlank(message = "Email is required")
    private String email;
}