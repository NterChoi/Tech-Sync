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

    /**
     * 네이버 뉴스 검색에 사용할 실제 질의어. null 이면 keywordName 으로 검색한다.
     * 예: 표시명 "Spring" → 검색어 "Spring 프레임워크" (계절 '봄' 기사 혼입 방지)
     */
    @Column(name = "SEARCH_QUERY", length = 100)
    private String searchQuery;

    @Builder
    public KeywordMaster(String keywordName, String category, String searchQuery) {
        this.keywordName = keywordName;
        this.category = category;
        this.searchQuery = searchQuery;
    }

    /** 네이버 검색에 쓸 질의어. searchQuery 가 지정돼 있으면 그것을, 없으면 표시명을 쓴다. */
    public String getEffectiveQuery() {
        return (searchQuery != null && !searchQuery.isBlank()) ? searchQuery : keywordName;
    }

    /** 검색어 보정값을 설정한다 (기존 row 마이그레이션용). */
    public void applySearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }
}
