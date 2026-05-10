package com.aws.dto;

import java.io.Serializable;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Email
    @NotBlank
    private String email;

    @NotBlank(message = "OTP is required")
    @Size(min = 6, max = 6)
    private String otp;

    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 20)
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@#$%^&+=!]).*$",
        message = "Password must contain uppercase, lowercase, number, special char"
    )
    private String newPassword;
}