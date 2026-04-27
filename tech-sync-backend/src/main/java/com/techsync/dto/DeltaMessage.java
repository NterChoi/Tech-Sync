package com.techsync.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;
import java.util.Map;

public record DeltaMessage(
        @NotEmpty(message = "ops는 비어있을 수 없습니다.")
        List<Map<String, Object>> ops,

        @NotNull(message = "clientSeqNo는 필수입니다.")
        @PositiveOrZero(message = "clientSeqNo는 0 이상이어야 합니다.")
        Long clientSeqNo
) {}
