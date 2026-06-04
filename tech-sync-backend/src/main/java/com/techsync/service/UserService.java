package com.techsync.service;

import com.techsync.dto.UpdateUserRequest;
import com.techsync.dto.UserResponse;

public interface UserService {

    UserResponse getMe(Long userId);

    UserResponse updateMe(Long userId, UpdateUserRequest request);
}
