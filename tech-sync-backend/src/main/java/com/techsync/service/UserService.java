package com.techsync.service;

import com.techsync.dto.UpdateUserRequest;
import com.techsync.dto.UserResponse;

public interface UserService {

    UserResponse getMe(Long userId);

    UserResponse updateMe(Long userId, UpdateUserRequest request);

    /** 현재 비밀번호 확인 후 새 비밀번호로 변경한다. 변경 시 기존 로그인 세션을 무효화한다. */
    void changePassword(Long userId, String currentPassword, String newPassword);

    /** 회원 탈퇴 — 계정 및 연관 데이터(구독 키워드/스크랩/워크스페이스 멤버십/토큰)를 정리한다. */
    void deleteAccount(Long userId);
}
