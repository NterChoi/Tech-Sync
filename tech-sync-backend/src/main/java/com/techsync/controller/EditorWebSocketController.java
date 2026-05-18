package com.techsync.controller;

import com.techsync.dto.CursorMessage;
import com.techsync.dto.DeltaMessage;
import com.techsync.exception.BusinessException;
import com.techsync.service.EditorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class EditorWebSocketController {

    private final EditorService editorService;

    @MessageMapping("/edit/{workspaceId}")
    public void edit(@DestinationVariable Long workspaceId,
                     @Payload @Valid DeltaMessage message,
                     Principal principal) {
        Long userId = requireUserId(principal);
        editorService.applyDelta(workspaceId, userId, message);
    }

    @MessageMapping("/cursor/{workspaceId}")
    public void cursor(@DestinationVariable Long workspaceId,
                       @Payload CursorMessage message,
                       Principal principal) {
        Long userId = requireUserId(principal);
        editorService.broadcastCursor(workspaceId, userId, message);
    }

    private Long requireUserId(Principal principal) {
        if (!(principal instanceof Authentication auth) || auth.getPrincipal() == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        return (Long) auth.getPrincipal();
    }
}
