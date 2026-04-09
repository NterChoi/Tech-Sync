package com.techsync.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "KEYWORD_MASTER")
@Getter
@NoArgsConstructor
public class KeywordMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "KEYWORD_ID")
    private Long keywordId;

    @Column(name = "KEYWORD_NAME", nullable = false, unique = true, length = 50)
    private String keywordName;

    @Column(name = "CATEGORY", nullable = false, length = 50)
    private String category;

    @Builder
    public KeywordMaster(String keywordName, String category) {
        this.keywordName = keywordName;
        this.category = category;
    }
}
