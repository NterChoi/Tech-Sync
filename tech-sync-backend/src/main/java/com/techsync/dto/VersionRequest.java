package com.techsync.dto;

import com.techsync.domain.DraftVersion.VersionType;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public record VersionRequest(
        @NotNull(message = "content는 필수입니다.")
        List<Map<String, Object>> content,

        @NotNull(message = "versionType은 필수입니다.")
        VersionType versionType
) {}
