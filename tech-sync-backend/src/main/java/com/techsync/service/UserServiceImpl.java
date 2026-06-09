package com.techsync.service;

import com.techsync.domain.User;
import com.techsync.dto.UpdateUserRequest;
import com.techsync.dto.UserResponse;
import com.techsync.exception.BusinessException;
import com.techsync.repository.KeywordRepository;
import com.techsync.repository.ScrapRepository;
import com.techsync.repository.UserRepository;
import com.techsync.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String REFRESH_TOKEN_PREFIX = "refresh:";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final KeywordRepository keywordRepository;
    private final ScrapRepository scrapRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getMe(Long userId) {
        return UserResponse.from(findUser(userId));
    }

    @Override
    @Transactional
    public UserResponse updateMe(Long userId, UpdateUserRequest request) {
        User user = findUser(userId);
        user.updateName(request.name());
        return UserResponse.from(user);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = findUser(userId);

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BusinessException("현재 비밀번호가 올바르지 않습니다.", HttpStatus.BAD_REQUEST);
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BusinessException("새 비밀번호가 기존 비밀번호와 동일합니다.", HttpStatus.BAD_REQUEST);
        }

        user.updatePassword(passwordEncoder.encode(newPassword));
        // 비밀번호 변경 시 기존 세션 무효화 → 재로그인 강제
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
    }

    @Override
    @Transactional
    public void deleteAccount(Long userId) {
        User user = findUser(userId);

        keywordRepository.deleteByUserId(userId);
        scrapRepository.deleteByUserId(userId);
        workspaceMemberRepository.deleteByUserId(userId);
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);

        userRepository.delete(user);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("존재하지 않는 사용자입니다.", HttpStatus.NOT_FOUND));
    }
}
