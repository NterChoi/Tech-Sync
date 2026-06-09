package com.techsync.repository;

import com.techsync.domain.Article;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

@RequiredArgsConstructor
public class ArticleRepositoryImpl implements ArticleRepositoryCustom {

    private static final String SOURCE_GEEK = "GEEK";
    private static final String SOURCE_NAVER = "NAVER";

    private final MongoTemplate mongoTemplate;

    @Override
    public Page<Article> findFeed(List<String> subscribedKeywords, String source, String keyword, Pageable pageable) {
        Criteria criteria = buildCriteria(subscribedKeywords, source, keyword);
        Query query = new Query(criteria).with(pageable);

        List<Article> content = mongoTemplate.find(query, Article.class);
        return PageableExecutionUtils.getPage(
                content,
                pageable,
                () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Article.class));
    }

    private Criteria buildCriteria(List<String> subscribedKeywords, String source, String keyword) {
        boolean hasKeyword = keyword != null && !keyword.isBlank();

        // 특정 키워드 필터 → NAVER 한정
        if (hasKeyword) {
            return Criteria.where("source").is(SOURCE_NAVER).and("keyword").is(keyword);
        }

        if (SOURCE_GEEK.equalsIgnoreCase(source)) {
            return Criteria.where("source").is(SOURCE_GEEK);
        }

        if (SOURCE_NAVER.equalsIgnoreCase(source)) {
            return Criteria.where("source").is(SOURCE_NAVER).and("keyword").in(safe(subscribedKeywords));
        }

        // 전체: GEEK 전부 + 구독 키워드 NAVER 기사
        return new Criteria().orOperator(
                Criteria.where("source").is(SOURCE_GEEK),
                Criteria.where("source").is(SOURCE_NAVER).and("keyword").in(safe(subscribedKeywords)));
    }

    /** in() 에 빈 컬렉션이 들어가면 아무것도 매칭하지 않도록 그대로 둔다 (null 만 방지). */
    private List<String> safe(List<String> keywords) {
        return keywords == null ? List.of() : keywords;
    }
}
