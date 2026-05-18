package com.techsync.dto;

public record CursorMessage(
        CursorRange range
) {
    public record CursorRange(
            int index,
            int length
    ) {}
}
