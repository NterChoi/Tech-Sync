package com.techsync.dto;

import com.techsync.domain.WorkspaceMember;

import java.time.LocalDateTime;

public record MemberResponse(
        Long memberId,
        Long userId,
        String userName,
        String email,
        String role,
        LocalDateTime joinedAt
) {
    public static MemberResponse of(WorkspaceMember member, String userName, String email) {
        return new MemberResponse(
                member.getMemberId(),
                member.getUserId(),
                userName,
                email,
                member.getRole(),
                member.getJoinedAt()
        );
    }
}
