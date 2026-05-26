package com.techsync.service;

import com.techsync.dto.CursorBroadcast;
import com.techsync.dto.CursorMessage;
import com.techsync.dto.DeltaBroadcast;
import com.techsync.dto.DeltaMessage;
import com.techsync.dto.SnapshotResponse;

import java.util.List;
import java.util.Map;

public interface EditorService {

    DeltaBroadcast applyDelta(Long workspaceId, Long userId, DeltaMessage message);

    CursorBroadcast broadcastCursor(Long workspaceId, Long userId, CursorMessage message);

    SnapshotResponse saveSnapshot(Long workspaceId, Long userId, List<Map<String, Object>> content);

    SnapshotResponse getSnapshot(Long workspaceId, Long userId);
}
