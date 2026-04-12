package com.techsync.service;

import com.techsync.dto.KeywordResponse;

import java.util.List;

public interface KeywordService {

    List<KeywordResponse> getRecommended();

    List<KeywordResponse> getMyKeywords(Long userId);

    void subscribe(Long userId, Long keywordMasterId);

    void unsubscribe(Long userId, Long keywordMasterId);
}
