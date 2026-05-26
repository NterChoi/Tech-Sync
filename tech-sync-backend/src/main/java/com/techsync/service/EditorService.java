package com.techsync.service;

import com.techsync.domain.DraftVersion.VersionType;
import com.techsync.dto.CursorBroadcast;
import com.techsync.dto.CursorMessage;
import com.techsync.dto.DeltaBroadcast;
import com.techsync.dto.DeltaMessage;
import com.techsync.dto.SnapshotResponse;
import com.techsync.dto.VersionResponse;

import java.util.List;
import java.util.Map;

public interface EditorService {

    DeltaBroadcast applyDelta(Long workspaceId, Long userId, DeltaMessage message);

    CursorBroadcast broadcastCursor(Long workspaceId, Long userId, CursorMessage message);

    SnapshotResponse saveSnapshot(Long workspaceId, Long userId, List<Map<String, Object>> content);

    SnapshotResponse getSnapshot(Long workspaceId, Long userId);

    VersionResponse saveVersion(Long workspaceId, Long userId,
                                List<Map<String, Object>> content, VersionType versionType);

    List<VersionResponse> getVersions(Long workspaceId, Long userId);

    VersionResponse getVersion(Long workspaceId, Long userId, Long versionNo);
}
