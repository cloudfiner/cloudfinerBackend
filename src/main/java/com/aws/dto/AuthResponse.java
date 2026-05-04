package com.aws.dto;

import java.util.List;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String email,
        String name,
        List<String> roles,   // ✅ change
        long expiresIn
) {}