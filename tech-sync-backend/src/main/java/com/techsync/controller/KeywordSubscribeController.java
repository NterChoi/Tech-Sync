package com.techsync.controller;

import com.techsync.dto.ApiResponse;
import com.techsync.dto.KeywordResponse;
import com.techsync.service.KeywordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/keywords")
@RequiredArgsConstructor
public class KeywordSubscribeController {

    private final KeywordService keywordService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<KeywordResponse>>> getRecommended() {
        return ResponseEntity.ok(ApiResponse.success(keywordService.getRecommended()));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<KeywordResponse>>> getMyKeywords(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.success(keywordService.getMyKeywords(userId)));
    }

    @PostMapping("/{keywordMasterId}")
    public ResponseEntity<ApiResponse<Void>> subscribe(@AuthenticationPrincipal Long userId,
                                                       @PathVariable Long keywordMasterId) {
        keywordService.subscribe(userId, keywordMasterId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(null));
    }

    @DeleteMapping("/{keywordMasterId}")
    public ResponseEntity<ApiResponse<Void>> unsubscribe(@AuthenticationPrincipal Long userId,
                                                         @PathVariable Long keywordMasterId) {
        keywordService.unsubscribe(userId, keywordMasterId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
