package com.techsync.service;

import com.techsync.config.JwtProvider;
import com.techsync.domain.User;
import com.techsync.dto.FindIdResponse;
import com.techsync.exception.BusinessException;
import com.techsync.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtProvider jwtProvider;
    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private AuthServiceImpl authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("hee687299@gmail.com")
                .password("oldEncoded")
                .name("최강현")
                .build();
        ReflectionTestUtils.setField(user, "userId", 7L);
    }

    @Test
    @DisplayName("아이디 찾기 - 이름으로 가입 이메일을 마스킹해 반환한다")
    void findEmailsByName_masks() {
        given(userRepository.findByName("최강현")).willReturn(List.of(user));

        FindIdResponse res = authService.findEmailsByName("최강현");

        assertThat(res.emails()).containsExactly("he*******@gmail.com");
    }

    @Test
    @DisplayName("비밀번호 재설정 성공 - 이메일+이름 일치 시 새 비밀번호로 인코딩 저장")
    void resetPassword_success() {
        given(userRepository.findByEmail("hee687299@gmail.com")).willReturn(Optional.of(user));
        given(passwordEncoder.encode("newPassw0rd")).willReturn("newEncoded");

        authService.resetPassword("hee687299@gmail.com", "최강현", "newPassw0rd");

        assertThat(user.getPassword()).isEqualTo("newEncoded");
        verify(redisTemplate).delete("refresh:7");
    }

    @Test
    @DisplayName("비밀번호 재설정 실패 - 이름 불일치 시 NOT_FOUND, 변경 없음")
    void resetPassword_nameMismatch() {
        given(userRepository.findByEmail("hee687299@gmail.com")).willReturn(Optional.of(user));

        assertThatThrownBy(() ->
                authService.resetPassword("hee687299@gmail.com", "다른이름", "newPassw0rd"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        assertThat(user.getPassword()).isEqualTo("oldEncoded");
        verify(passwordEncoder, never()).encode(anyString());
    }
}
