package com.techsync.repository;

import com.techsync.domain.Article;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ArticleRepository extends MongoRepository<Article, String> {

    boolean existsBySourceId(String sourceId);
}
