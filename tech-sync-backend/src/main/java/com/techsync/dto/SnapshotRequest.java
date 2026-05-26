package com.techsync.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public record SnapshotRequest(
        @NotNull(message = "content는 필수입니다.")
        List<Map<String, Object>> content
) {}
