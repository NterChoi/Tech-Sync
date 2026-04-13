package com.techsync.service;

import com.techsync.dto.ArticleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FeedService {

    Page<ArticleResponse> getFeed(Long userId, Pageable pageable);

    Page<ArticleResponse> getScraps(Long userId, Pageable pageable);

    void scrap(Long userId, String articleId);

    void unscrap(Long userId, String articleId);
}
