package com.techsync.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "ARTICLE")
@Getter
@NoArgsConstructor
public class Article {

    @Id
    private String id;

    @Indexed(unique = true)
    @Field("SOURCE_ID")
    private String sourceId;  // 긱뉴스: <id>, 네이버: 링크 URL

    @Field("SOURCE")
    private String source;    // GEEK | NAVER

    @Field("TITLE")
    private String title;

    @Field("LINK")
    private String link;

    @Field("DESCRIPTION")
    private String description;

    @Field("KEYWORD")
    private String keyword;  // 네이버 수집 시 사용한 키워드 (GEEK은 null)

    @Field("PUBLISHED_AT")
    private LocalDateTime publishedAt;

    @Field("COLLECTED_AT")
    private LocalDateTime collectedAt;

    @Builder
    public Article(String sourceId, String source, String title, String link,
                   String description, String keyword, LocalDateTime publishedAt, LocalDateTime collectedAt) {
        this.sourceId = sourceId;
        this.source = source;
        this.title = title;
        this.link = link;
        this.description = description;
        this.keyword = keyword;
        this.publishedAt = publishedAt;
        this.collectedAt = collectedAt;
    }
}
