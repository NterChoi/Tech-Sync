package com.techsync.service;

import com.techsync.dto.*;

import java.util.List;

public interface WorkspaceService {

    WorkspaceResponse create(Long userId, WorkspaceRequest request);

    List<WorkspaceResponse> getMyWorkspaces(Long userId);

    WorkspaceDetailResponse getDetail(Long userId, Long workspaceId);

    WorkspaceResponse update(Long userId, Long workspaceId, WorkspaceRequest request);

    void delete(Long userId, Long workspaceId);

    MemberResponse inviteMember(Long userId, Long workspaceId, MemberInviteRequest request);

    void removeMember(Long userId, Long workspaceId, Long targetUserId);
}
