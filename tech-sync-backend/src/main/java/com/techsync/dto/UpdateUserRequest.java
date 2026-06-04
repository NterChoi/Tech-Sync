package com.techsync.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @NotBlank(message = "이름은 비어 있을 수 없습니다.")
        @Size(max = 50, message = "이름은 50자를 넘을 수 없습니다.")
        String name
) {
}
