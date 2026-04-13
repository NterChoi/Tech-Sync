package com.techsync.controller;

import com.techsync.dto.ApiResponse;
import com.techsync.dto.ArticleResponse;
import com.techsync.service.FeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ArticleResponse>>> getFeed(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 20, sort = "publishedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(feedService.getFeed(userId, pageable)));
    }

    @GetMapping("/scraps")
    public ResponseEntity<ApiResponse<Page<ArticleResponse>>> getScraps(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 20, sort = "publishedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(feedService.getScraps(userId, pageable)));
    }

    @PostMapping("/{articleId}/scrap")
    public ResponseEntity<ApiResponse<Void>> scrap(
            @AuthenticationPrincipal Long userId,
            @PathVariable String articleId) {
        feedService.scrap(userId, articleId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(null));
    }

    @DeleteMapping("/{articleId}/scrap")
    public ResponseEntity<ApiResponse<Void>> unscrap(
            @AuthenticationPrincipal Long userId,
            @PathVariable String articleId) {
        feedService.unscrap(userId, articleId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
