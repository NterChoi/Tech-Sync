package com.techsync.dto;

import com.techsync.domain.User;

import java.time.LocalDateTime;

public record UserResponse(Long userId, String email, String name, LocalDateTime createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getUserId(), user.getEmail(), user.getName(), user.getCreatedAt());
    }
}
