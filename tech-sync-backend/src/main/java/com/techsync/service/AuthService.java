package com.techsync.service;

import com.techsync.dto.AuthResponse;
import com.techsync.dto.FindIdResponse;
import com.techsync.dto.LoginRequest;
import com.techsync.dto.SignupRequest;

public interface AuthService {

    void signup(SignupRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(String refreshToken);

    void logout(String refreshToken);

    /** 이름으로 가입 이메일(마스킹)을 조회한다. */
    FindIdResponse findEmailsByName(String name);

    /** 이메일+이름 본인확인 후 비밀번호를 즉시 재설정한다. */
    void resetPassword(String email, String name, String newPassword);
}
