package com.techsync.dto;

import com.techsync.domain.Keyword;
import com.techsync.domain.KeywordMaster;

public record KeywordResponse(Long id, String keywordName, String category) {

    public static KeywordResponse from(Keyword keyword) {
        return new KeywordResponse(keyword.getKeywordId(), keyword.getKeywordName(), null);
    }

    public static KeywordResponse from(KeywordMaster keywordMaster) {
        return new KeywordResponse(keywordMaster.getKeywordId(), keywordMaster.getKeywordName(), keywordMaster.getCategory());
    }
}
