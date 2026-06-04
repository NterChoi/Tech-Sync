package com.techsync.service;

import com.techsync.domain.User;
import com.techsync.dto.UpdateUserRequest;
import com.techsync.dto.UserResponse;
import com.techsync.exception.BusinessException;
import com.techsync.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

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
}
