package com.techsync.repository;

import com.techsync.domain.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface ArticleRepository extends MongoRepository<Article, String> {

    boolean existsBySourceId(String sourceId);

    // 구독 키워드 기반 NAVER 기사 + 전체 GEEK 기사 통합 피드
    @Query("{ '$or': [ { 'SOURCE': 'GEEK' }, { 'SOURCE': 'NAVER', 'KEYWORD': { '$in': ?0 } } ] }")
    Page<Article> findFeedArticles(List<String> keywords, Pageable pageable);

    // 스크랩한 기사 목록 조회
    Page<Article> findByIdIn(List<String> ids, Pageable pageable);
}
