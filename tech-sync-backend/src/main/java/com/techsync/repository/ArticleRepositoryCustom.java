package com.techsync.repository;

import com.techsync.domain.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ArticleRepositoryCustom {

    /**
     * 소스/키워드 필터를 반영한 피드 조회.
     *
     * @param subscribedKeywords 사용자가 구독한 키워드 목록 (NAVER 기사 필터 기준)
     * @param source             "GEEK" | "NAVER" | null(전체)
     * @param keyword            특정 키워드만 볼 때 지정 (NAVER 한정), null 이면 미적용
     */
    Page<Article> findFeed(List<String> subscribedKeywords, String source, String keyword, Pageable pageable);
}
