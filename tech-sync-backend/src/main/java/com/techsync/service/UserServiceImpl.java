package com.techsync.service;

import com.techsync.domain.User;
import com.techsync.dto.UpdateUserRequest;
import com.techsync.dto.UserResponse;
import com.techsync.exception.BusinessException;
import com.techsync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

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

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("존재하지 않는 사용자입니다.", HttpStatus.NOT_FOUND));
    }
}
