package com.aws.dto;

import java.io.Serializable;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

    @NotBlank
    @Size(min = 2, max = 50)
    String name,

    @NotBlank
    @Email(message = "Invalid email format")
    String email,

    @NotBlank
    @Size(min = 6, max = 50, message = "Password must be 6-50 chars")
    String password

) implements Serializable {

    private static final long serialVersionUID = 1L;
}