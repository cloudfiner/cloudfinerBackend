package com.aws.dto;

import java.io.Serializable;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.*;

public record LoginRequest(

    @NotBlank(message = "Email must not be empty")
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    String email,

    @NotBlank(message = "Password must not be empty")
    @Size(min = 6, max = 50, message = "Password must be between 6 and 50 characters")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
        message = "Password must contain at least one letter and one number"
    )
    String password

) implements Serializable {

    private static final long serialVersionUID = 1L;
}