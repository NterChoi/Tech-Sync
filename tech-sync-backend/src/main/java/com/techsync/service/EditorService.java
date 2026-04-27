package com.techsync.service;

import com.techsync.dto.DeltaBroadcast;
import com.techsync.dto.DeltaMessage;

public interface EditorService {

    DeltaBroadcast applyDelta(Long workspaceId, Long userId, DeltaMessage message);
}
