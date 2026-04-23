package com.techsync.service;

import com.techsync.domain.User;
import com.techsync.domain.Workspace;
import com.techsync.domain.WorkspaceMember;
import com.techsync.dto.*;
import com.techsync.exception.BusinessException;
import com.techsync.repository.UserRepository;
import com.techsync.repository.WorkspaceMemberRepository;
import com.techsync.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkspaceServiceImpl implements WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public WorkspaceResponse create(Long userId, WorkspaceRequest request) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        Workspace workspace = workspaceRepository.save(
                Workspace.builder()
                        .workspaceName(request.name())
                        .ownerId(userId)
                        .build()
        );

        workspaceMemberRepository.save(
                WorkspaceMember.builder()
                        .workspaceId(workspace.getWorkspaceId())
                        .userId(userId)
                        .role("OWNER")
                        .build()
        );

        return WorkspaceResponse.of(workspace, owner.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceResponse> getMyWorkspaces(Long userId) {
        List<WorkspaceMember> memberships = workspaceMemberRepository.findByUserId(userId);
        List<Long> workspaceIds = memberships.stream()
                .map(WorkspaceMember::getWorkspaceId)
                .collect(Collectors.toList());

        if (workspaceIds.isEmpty()) {
            return List.of();
        }

        List<Workspace> workspaces = workspaceRepository.findAllById(workspaceIds);

        List<Long> ownerIds = workspaces.stream()
                .map(Workspace::getOwnerId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> ownerNames = userRepository.findAllById(ownerIds).stream()
                .collect(Collectors.toMap(User::getUserId, User::getName));

        return workspaces.stream()
                .map(ws -> WorkspaceResponse.of(ws, ownerNames.getOrDefault(ws.getOwnerId(), "")))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public WorkspaceDetailResponse getDetail(Long userId, Long workspaceId) {
        Workspace workspace = findWorkspaceOrThrow(workspaceId);
        validateMember(workspaceId, userId);

        User owner = userRepository.findById(workspace.getOwnerId())
                .orElseThrow(() -> new BusinessException("소유자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        List<WorkspaceMember> members = workspaceMemberRepository.findByWorkspaceId(workspaceId);
        List<Long> userIds = members.stream()
                .map(WorkspaceMember::getUserId)
                .collect(Collectors.toList());
        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getUserId, u -> u));

        List<MemberResponse> memberResponses = members.stream()
                .map(m -> {
                    User user = userMap.get(m.getUserId());
                    return MemberResponse.of(m,
                            user != null ? user.getName() : "",
                            user != null ? user.getEmail() : "");
                })
                .collect(Collectors.toList());

        return WorkspaceDetailResponse.of(workspace, owner.getName(), memberResponses);
    }

    @Override
    @Transactional
    public WorkspaceResponse update(Long userId, Long workspaceId, WorkspaceRequest request) {
        Workspace workspace = findWorkspaceOrThrow(workspaceId);
        validateOwner(workspace, userId);

        workspace.updateName(request.name());

        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        return WorkspaceResponse.of(workspace, owner.getName());
    }

    @Override
    @Transactional
    public void delete(Long userId, Long workspaceId) {
        Workspace workspace = findWorkspaceOrThrow(workspaceId);
        validateOwner(workspace, userId);

        workspaceMemberRepository.findByWorkspaceId(workspaceId)
                .forEach(workspaceMemberRepository::delete);
        workspaceRepository.delete(workspace);
    }

    @Override
    @Transactional
    public MemberResponse inviteMember(Long userId, Long workspaceId, MemberInviteRequest request) {
        Workspace workspace = findWorkspaceOrThrow(workspaceId);
        validateOwner(workspace, userId);

        User targetUser = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException("해당 이메일의 사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        if (workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, targetUser.getUserId())) {
            throw new BusinessException("이미 워크스페이스 멤버입니다.", HttpStatus.CONFLICT);
        }

        String role = request.role();
        if (!"EDITOR".equals(role) && !"VIEWER".equals(role)) {
            throw new BusinessException("역할은 EDITOR 또는 VIEWER만 가능합니다.", HttpStatus.BAD_REQUEST);
        }

        WorkspaceMember member = workspaceMemberRepository.save(
                WorkspaceMember.builder()
                        .workspaceId(workspaceId)
                        .userId(targetUser.getUserId())
                        .role(role)
                        .build()
        );

        return MemberResponse.of(member, targetUser.getName(), targetUser.getEmail());
    }

    @Override
    @Transactional
    public void removeMember(Long userId, Long workspaceId, Long targetUserId) {
        Workspace workspace = findWorkspaceOrThrow(workspaceId);

        if (workspace.getOwnerId().equals(targetUserId)) {
            throw new BusinessException("워크스페이스 소유자는 제거할 수 없습니다.", HttpStatus.BAD_REQUEST);
        }

        // 본인 탈퇴이거나 OWNER가 제거하는 경우만 허용
        if (!userId.equals(targetUserId)) {
            validateOwner(workspace, userId);
        }

        WorkspaceMember member = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, targetUserId)
                .orElseThrow(() -> new BusinessException("해당 멤버를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        workspaceMemberRepository.delete(member);
    }

    private Workspace findWorkspaceOrThrow(Long workspaceId) {
        return workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new BusinessException("워크스페이스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
    }

    private void validateOwner(Workspace workspace, Long userId) {
        if (!workspace.getOwnerId().equals(userId)) {
            throw new BusinessException("워크스페이스 소유자만 수행할 수 있습니다.", HttpStatus.FORBIDDEN);
        }
    }

    private void validateMember(Long workspaceId, Long userId) {
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new BusinessException("워크스페이스 멤버가 아닙니다.", HttpStatus.FORBIDDEN);
        }
    }
}
