package com.techsync.dto;

import jakarta.validation.constraints.NotBlank;

public record FindIdRequest(
        @NotBlank(message = "이름을 입력해주세요.")
        String name
) {
}
