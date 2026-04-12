package com.techsync.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "KEYWORD",
        uniqueConstraints = @UniqueConstraint(columnNames = {"USER_ID", "KEYWORD_NAME"}))
@Getter
@NoArgsConstructor
public class Keyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "KEYWORD_ID")
    private Long keywordId;

    @Column(name = "USER_ID", nullable = false)
    private Long userId;

    @Column(name = "KEYWORD_NAME", nullable = false, length = 50)
    private String keywordName;

    @Builder
    public Keyword(Long userId, String keywordName) {
        this.userId = userId;
        this.keywordName = keywordName;
    }
}
