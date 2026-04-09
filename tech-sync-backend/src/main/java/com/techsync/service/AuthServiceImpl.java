package com.techsync.service;

import com.techsync.config.JwtProvider;
import com.techsync.domain.User;
import com.techsync.dto.AuthResponse;
import com.techsync.dto.LoginRequest;
import com.techsync.dto.SignupRequest;
import com.techsync.exception.BusinessException;
import com.techsync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String REFRESH_TOKEN_PREFIX = "refresh:";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final StringRedisTemplate redisTemplate;

    @Override
    @Transactional
    public void signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("이미 사용 중인 이메일입니다.", HttpStatus.CONFLICT);
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .build();

        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException("이메일 또는 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException("이메일 또는 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED);
        }

        String accessToken = jwtProvider.createAccessToken(user.getUserId(), user.getEmail());
        String refreshToken = jwtProvider.createRefreshToken(user.getUserId(), user.getEmail());

        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + user.getUserId(),
                refreshToken,
                Duration.ofMillis(jwtProvider.getRefreshExpiration())
        );

        return new AuthResponse(accessToken, refreshToken, user.getUserId(), user.getName());
    }

    @Override
    public AuthResponse refresh(String refreshToken) {
        if (!jwtProvider.isValid(refreshToken)) {
            throw new BusinessException("유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED);
        }

        Long userId = jwtProvider.getUserId(refreshToken);
        String email = jwtProvider.getEmail(refreshToken);

        String stored = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + userId);
        if (stored == null || !stored.equals(refreshToken)) {
            throw new BusinessException("유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        String newAccessToken = jwtProvider.createAccessToken(userId, email);
        String newRefreshToken = jwtProvider.createRefreshToken(userId, email);

        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + userId,
                newRefreshToken,
                Duration.ofMillis(jwtProvider.getRefreshExpiration())
        );

        return new AuthResponse(newAccessToken, newRefreshToken, userId, user.getName());
    }

    @Override
    public void logout(String refreshToken) {
        if (!jwtProvider.isValid(refreshToken)) {
            throw new BusinessException("유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED);
        }
        Long userId = jwtProvider.getUserId(refreshToken);
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
    }
}
