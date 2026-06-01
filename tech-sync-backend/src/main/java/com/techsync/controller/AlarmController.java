package com.techsync.controller;

import com.techsync.dto.ApiResponse;
import com.techsync.dto.AlarmResponse;
import com.techsync.service.AlarmService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/alarm")
@RequiredArgsConstructor
public class AlarmController {

    private final AlarmService alarmService;

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal Long userId) {
        return alarmService.subscribe(userId);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AlarmResponse>>> getMyAlarms(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.success(alarmService.getMyAlarms(userId)));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.success(alarmService.getUnreadCount(userId)));
    }

    @PatchMapping("/{alarmId}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(
            @AuthenticationPrincipal Long userId,
            @PathVariable String alarmId) {
        alarmService.markRead(userId, alarmId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
