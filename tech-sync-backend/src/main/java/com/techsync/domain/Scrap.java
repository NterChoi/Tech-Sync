package com.techsync.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "SCRAP",
        uniqueConstraints = @UniqueConstraint(columnNames = {"USER_ID", "ARTICLE_ID"}))
@Getter
@NoArgsConstructor
public class Scrap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SCRAP_ID")
    private Long scrapId;

    @Column(name = "USER_ID", nullable = false)
    private Long userId;

    @Column(name = "ARTICLE_ID", nullable = false, length = 100)
    private String articleId;

    @Builder
    public Scrap(Long userId, String articleId) {
        this.userId = userId;
        this.articleId = articleId;
    }
}
