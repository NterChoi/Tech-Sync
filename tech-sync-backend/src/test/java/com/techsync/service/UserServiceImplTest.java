package com.techsync.service;

import com.techsync.domain.User;
import com.techsync.dto.UpdateUserRequest;
import com.techsync.dto.UserResponse;
import com.techsync.exception.BusinessException;
import com.techsync.repository.KeywordRepository;
import com.techsync.repository.ScrapRepository;
import com.techsync.repository.UserRepository;
import com.techsync.repository.WorkspaceMemberRepository;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private KeywordRepository keywordRepository;
    @Mock
    private ScrapRepository scrapRepository;
    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("me@test.com")
                .password("pwd")
                .name("기존이름")
                .build();
        ReflectionTestUtils.setField(user, "userId", 1L);
    }

    @Test
    @DisplayName("내 정보 수정 성공 - 이름이 변경된다")
    void updateMe_success() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        UserResponse response = userService.updateMe(1L, new UpdateUserRequest("새이름"));

        assertThat(response.name()).isEqualTo("새이름");
        assertThat(response.email()).isEqualTo("me@test.com");
        assertThat(user.getName()).isEqualTo("새이름");
    }

    @Test
    @DisplayName("내 정보 수정 실패 - 존재하지 않는 사용자")
    void updateMe_userNotFound() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateMe(99L, new UpdateUserRequest("새이름")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("존재하지 않는 사용자")
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("비밀번호 변경 성공 - 현재 비번 확인 후 새 비번 인코딩 저장 + 세션 무효화")
    void changePassword_success() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("pwd", "pwd")).willReturn(true);     // 현재 비번 일치
        given(passwordEncoder.matches("newPassw0rd", "pwd")).willReturn(false); // 기존과 다름
        given(passwordEncoder.encode("newPassw0rd")).willReturn("newEncoded");

        userService.changePassword(1L, "pwd", "newPassw0rd");

        assertThat(user.getPassword()).isEqualTo("newEncoded");
        verify(redisTemplate).delete("refresh:1");
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 현재 비밀번호 불일치 시 BAD_REQUEST, 변경 없음")
    void changePassword_wrongCurrent() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong", "pwd")).willReturn(false);

        assertThatThrownBy(() -> userService.changePassword(1L, "wrong", "newPassw0rd"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        assertThat(user.getPassword()).isEqualTo("pwd");
        verify(passwordEncoder, never()).encode(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("회원 탈퇴 - 연관 데이터 정리 후 계정 삭제")
    void deleteAccount_cascades() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        userService.deleteAccount(1L);

        verify(keywordRepository).deleteByUserId(1L);
        verify(scrapRepository).deleteByUserId(1L);
        verify(workspaceMemberRepository).deleteByUserId(1L);
        verify(redisTemplate).delete("refresh:1");
        verify(userRepository).delete(user);
    }
}
