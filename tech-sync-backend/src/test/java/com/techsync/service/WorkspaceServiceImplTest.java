package com.techsync.service;

import com.techsync.domain.User;
import com.techsync.domain.Workspace;
import com.techsync.domain.WorkspaceMember;
import com.techsync.dto.MemberInviteRequest;
import com.techsync.dto.MemberResponse;
import com.techsync.dto.WorkspaceDetailResponse;
import com.techsync.dto.WorkspaceRequest;
import com.techsync.dto.WorkspaceResponse;
import com.techsync.exception.BusinessException;
import com.techsync.repository.UserRepository;
import com.techsync.repository.WorkspaceMemberRepository;
import com.techsync.repository.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceImplTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WorkspaceServiceImpl workspaceService;

    private User owner;
    private User memberUser;
    private Workspace workspace;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .email("owner@test.com")
                .password("pwd")
                .name("Owner")
                .build();
        ReflectionTestUtils.setField(owner, "userId", 1L);

        memberUser = User.builder()
                .email("member@test.com")
                .password("pwd")
                .name("Member")
                .build();
        ReflectionTestUtils.setField(memberUser, "userId", 2L);

        workspace = Workspace.builder()
                .workspaceName("My WS")
                .ownerId(1L)
                .build();
        ReflectionTestUtils.setField(workspace, "workspaceId", 100L);
    }

    // ==================== 정상 케이스 ====================

    @Test
    @DisplayName("create: 워크스페이스 저장 + OWNER 멤버 자동 등록")
    void create_success() {
        given(userRepository.findById(1L)).willReturn(Optional.of(owner));
        given(workspaceRepository.save(any(Workspace.class))).willReturn(workspace);

        WorkspaceResponse response = workspaceService.create(1L, new WorkspaceRequest("My WS"));

        assertThat(response.workspaceId()).isEqualTo(100L);
        assertThat(response.ownerId()).isEqualTo(1L);
        assertThat(response.ownerName()).isEqualTo("Owner");

        ArgumentCaptor<WorkspaceMember> captor = ArgumentCaptor.forClass(WorkspaceMember.class);
        verify(workspaceMemberRepository).save(captor.capture());
        WorkspaceMember saved = captor.getValue();
        assertThat(saved.getWorkspaceId()).isEqualTo(100L);
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getRole()).isEqualTo("OWNER");
    }

    @Test
    @DisplayName("getMyWorkspaces: 멤버십이 없으면 빈 리스트를 반환하고 workspace 쿼리는 실행하지 않는다")
    void getMyWorkspaces_empty() {
        given(workspaceMemberRepository.findByUserId(99L)).willReturn(List.of());

        List<WorkspaceResponse> result = workspaceService.getMyWorkspaces(99L);

        assertThat(result).isEmpty();
        verify(workspaceRepository, never()).findAllById(any());
    }

    @Test
    @DisplayName("getDetail: 멤버인 유저가 조회하면 멤버 목록이 포함된 상세를 반환한다")
    void getDetail_success() {
        WorkspaceMember ownerMember = WorkspaceMember.builder()
                .workspaceId(100L).userId(1L).role("OWNER").build();
        ReflectionTestUtils.setField(ownerMember, "memberId", 10L);

        WorkspaceMember editor = WorkspaceMember.builder()
                .workspaceId(100L).userId(2L).role("EDITOR").build();
        ReflectionTestUtils.setField(editor, "memberId", 11L);

        given(workspaceRepository.findById(100L)).willReturn(Optional.of(workspace));
        given(workspaceMemberRepository.existsByWorkspaceIdAndUserId(100L, 2L)).willReturn(true);
        given(userRepository.findById(1L)).willReturn(Optional.of(owner));
        given(workspaceMemberRepository.findByWorkspaceId(100L))
                .willReturn(List.of(ownerMember, editor));
        given(userRepository.findAllById(List.of(1L, 2L)))
                .willReturn(List.of(owner, memberUser));

        WorkspaceDetailResponse response = workspaceService.getDetail(2L, 100L);

        assertThat(response.workspaceId()).isEqualTo(100L);
        assertThat(response.ownerName()).isEqualTo("Owner");
        assertThat(response.members()).hasSize(2);
        assertThat(response.members())
                .extracting(MemberResponse::role)
                .containsExactlyInAnyOrder("OWNER", "EDITOR");
        assertThat(response.members())
                .extracting(MemberResponse::email)
                .containsExactlyInAnyOrder("owner@test.com", "member@test.com");
    }

    @Test
    @DisplayName("update: OWNER가 이름을 변경할 수 있다")
    void update_success() {
        given(workspaceRepository.findById(100L)).willReturn(Optional.of(workspace));
        given(userRepository.findById(1L)).willReturn(Optional.of(owner));

        WorkspaceResponse response = workspaceService.update(1L, 100L,
                new WorkspaceRequest("Updated"));

        assertThat(workspace.getWorkspaceName()).isEqualTo("Updated");
        assertThat(response.workspaceName()).isEqualTo("Updated");
    }

    @Test
    @DisplayName("delete: OWNER가 삭제하면 멤버가 모두 제거된 뒤 워크스페이스가 삭제된다")
    void delete_success() {
        WorkspaceMember ownerMember = WorkspaceMember.builder()
                .workspaceId(100L).userId(1L).role("OWNER").build();
        WorkspaceMember editor = WorkspaceMember.builder()
                .workspaceId(100L).userId(2L).role("EDITOR").build();

        given(workspaceRepository.findById(100L)).willReturn(Optional.of(workspace));
        given(workspaceMemberRepository.findByWorkspaceId(100L))
                .willReturn(List.of(ownerMember, editor));

        workspaceService.delete(1L, 100L);

        verify(workspaceMemberRepository, times(2)).delete(any(WorkspaceMember.class));
        verify(workspaceRepository).delete(workspace);
    }

    @Test
    @DisplayName("inviteMember: OWNER가 EDITOR 역할로 초대하면 멤버가 저장된다")
    void inviteMember_success() {
        WorkspaceMember saved = WorkspaceMember.builder()
                .workspaceId(100L).userId(2L).role("EDITOR").build();
        ReflectionTestUtils.setField(saved, "memberId", 20L);

        given(workspaceRepository.findById(100L)).willReturn(Optional.of(workspace));
        given(userRepository.findByEmail("member@test.com")).willReturn(Optional.of(memberUser));
        given(workspaceMemberRepository.existsByWorkspaceIdAndUserId(100L, 2L)).willReturn(false);
        given(workspaceMemberRepository.save(any(WorkspaceMember.class))).willReturn(saved);

        MemberResponse response = workspaceService.inviteMember(1L, 100L,
                new MemberInviteRequest("member@test.com", "EDITOR"));

        assertThat(response.memberId()).isEqualTo(20L);
        assertThat(response.userId()).isEqualTo(2L);
        assertThat(response.role()).isEqualTo("EDITOR");
        assertThat(response.email()).isEqualTo("member@test.com");
    }

    @Test
    @DisplayName("removeMember: 일반 멤버가 본인 탈퇴하면 OWNER 검증 없이 제거된다")
    void removeMember_selfWithdraw_success() {
        WorkspaceMember editor = WorkspaceMember.builder()
                .workspaceId(100L).userId(2L).role("EDITOR").build();

        given(workspaceRepository.findById(100L)).willReturn(Optional.of(workspace));
        given(workspaceMemberRepository.findByWorkspaceIdAndUserId(100L, 2L))
                .willReturn(Optional.of(editor));

        workspaceService.removeMember(2L, 100L, 2L);

        verify(workspaceMemberRepository).delete(editor);
    }

    // ==================== 실패 케이스 ====================

    @Test
    @DisplayName("update: OWNER가 아닌 유저가 수정을 시도하면 403 FORBIDDEN")
    void update_notOwner_forbidden() {
        given(workspaceRepository.findById(100L)).willReturn(Optional.of(workspace));

        assertThatThrownBy(() ->
                workspaceService.update(2L, 100L, new WorkspaceRequest("X")))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("delete: OWNER가 아닌 유저가 삭제를 시도하면 403 FORBIDDEN")
    void delete_notOwner_forbidden() {
        given(workspaceRepository.findById(100L)).willReturn(Optional.of(workspace));

        assertThatThrownBy(() -> workspaceService.delete(2L, 100L))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("getDetail: 워크스페이스 멤버가 아닌 유저가 조회하면 403 FORBIDDEN")
    void getDetail_notMember_forbidden() {
        given(workspaceRepository.findById(100L)).willReturn(Optional.of(workspace));
        given(workspaceMemberRepository.existsByWorkspaceIdAndUserId(100L, 99L))
                .willReturn(false);

        assertThatThrownBy(() -> workspaceService.getDetail(99L, 100L))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("inviteMember: 존재하지 않는 이메일로 초대하면 404 NOT_FOUND")
    void inviteMember_emailNotFound_notFound() {
        given(workspaceRepository.findById(100L)).willReturn(Optional.of(workspace));
        given(userRepository.findByEmail("ghost@test.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> workspaceService.inviteMember(1L, 100L,
                new MemberInviteRequest("ghost@test.com", "EDITOR")))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("inviteMember: 이미 워크스페이스 멤버인 유저를 초대하면 409 CONFLICT")
    void inviteMember_alreadyMember_conflict() {
        given(workspaceRepository.findById(100L)).willReturn(Optional.of(workspace));
        given(userRepository.findByEmail("member@test.com")).willReturn(Optional.of(memberUser));
        given(workspaceMemberRepository.existsByWorkspaceIdAndUserId(100L, 2L)).willReturn(true);

        assertThatThrownBy(() -> workspaceService.inviteMember(1L, 100L,
                new MemberInviteRequest("member@test.com", "EDITOR")))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("inviteMember: 역할이 EDITOR/VIEWER가 아니면 400 BAD_REQUEST")
    void inviteMember_invalidRole_badRequest() {
        given(workspaceRepository.findById(100L)).willReturn(Optional.of(workspace));
        given(userRepository.findByEmail("member@test.com")).willReturn(Optional.of(memberUser));
        given(workspaceMemberRepository.existsByWorkspaceIdAndUserId(100L, 2L)).willReturn(false);

        assertThatThrownBy(() -> workspaceService.inviteMember(1L, 100L,
                new MemberInviteRequest("member@test.com", "ADMIN")))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("removeMember: OWNER를 제거하려 하면 400 BAD_REQUEST")
    void removeMember_ownerRemoval_badRequest() {
        given(workspaceRepository.findById(100L)).willReturn(Optional.of(workspace));

        assertThatThrownBy(() -> workspaceService.removeMember(2L, 100L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
