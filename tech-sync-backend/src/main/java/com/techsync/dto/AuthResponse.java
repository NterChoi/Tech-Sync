package com.techsync.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        Long userId,
        String name
) {}
