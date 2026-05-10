package com.aws.dto;

import java.io.Serializable;
import java.util.List;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String email,
        String name,
        List<String> roles,
        long expiresIn
) implements Serializable {

    private static final long serialVersionUID = 1L;
}