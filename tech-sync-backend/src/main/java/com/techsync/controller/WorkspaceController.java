package com.techsync.controller;

import com.techsync.dto.*;
import com.techsync.service.WorkspaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @PostMapping
    public ResponseEntity<ApiResponse<WorkspaceResponse>> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody WorkspaceRequest request) {
        WorkspaceResponse response = workspaceService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkspaceResponse>>> getMyWorkspaces(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.success(workspaceService.getMyWorkspaces(userId)));
    }

    @GetMapping("/{workspaceId}")
    public ResponseEntity<ApiResponse<WorkspaceDetailResponse>> getDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long workspaceId) {
        return ResponseEntity.ok(ApiResponse.success(workspaceService.getDetail(userId, workspaceId)));
    }

    @PutMapping("/{workspaceId}")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> update(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long workspaceId,
            @Valid @RequestBody WorkspaceRequest request) {
        return ResponseEntity.ok(ApiResponse.success(workspaceService.update(userId, workspaceId, request)));
    }

    @DeleteMapping("/{workspaceId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long workspaceId) {
        workspaceService.delete(userId, workspaceId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{workspaceId}/members")
    public ResponseEntity<ApiResponse<MemberResponse>> inviteMember(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long workspaceId,
            @Valid @RequestBody MemberInviteRequest request) {
        MemberResponse response = workspaceService.inviteMember(userId, workspaceId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @DeleteMapping("/{workspaceId}/members/{targetUserId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long workspaceId,
            @PathVariable Long targetUserId) {
        workspaceService.removeMember(userId, workspaceId, targetUserId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
