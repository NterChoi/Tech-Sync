package com.techsync.service;

import com.techsync.dto.AuthResponse;
import com.techsync.dto.LoginRequest;
import com.techsync.dto.SignupRequest;

public interface AuthService {

    void signup(SignupRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(String refreshToken);
}
