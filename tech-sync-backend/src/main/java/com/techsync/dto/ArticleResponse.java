package com.techsync.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.techsync.domain.Article;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ArticleResponse {

    private final String id;
    private final String source;
    private final String title;
    private final String link;
    private final String description;
    private final String keyword;
    private final LocalDateTime publishedAt;

    @JsonProperty("isScraped")
    private final boolean isScraped;

    public ArticleResponse(Article article, boolean isScraped) {
        this.id = article.getId();
        this.source = article.getSource();
        this.title = article.getTitle();
        this.link = article.getLink();
        this.description = article.getDescription();
        this.keyword = article.getKeyword();
        this.publishedAt = article.getPublishedAt();
        this.isScraped = isScraped;
    }
}
