package com.techsync.repository;

import com.techsync.domain.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ArticleRepository extends MongoRepository<Article, String>, ArticleRepositoryCustom {

    boolean existsBySourceId(String sourceId);

    // 스크랩한 기사 목록 조회
    Page<Article> findByIdIn(List<String> ids, Pageable pageable);
}
