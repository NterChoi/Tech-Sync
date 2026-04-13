package com.techsync.service;

import com.techsync.domain.Scrap;
import com.techsync.dto.ArticleResponse;
import com.techsync.exception.BusinessException;
import com.techsync.repository.ArticleRepository;
import com.techsync.repository.KeywordRepository;
import com.techsync.repository.ScrapRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {

    private final ArticleRepository articleRepository;
    private final KeywordRepository keywordRepository;
    private final ScrapRepository scrapRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ArticleResponse> getFeed(Long userId, Pageable pageable) {
        List<String> keywords = keywordRepository.findByUserId(userId).stream()
                .map(k -> k.getKeywordName())
                .collect(Collectors.toList());

        Set<String> scrapedIds = scrapRepository.findArticleIdByUserId(userId)
                .stream().collect(Collectors.toSet());

        return articleRepository.findFeedArticles(keywords, pageable)
                .map(article -> new ArticleResponse(article, scrapedIds.contains(article.getId())));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ArticleResponse> getScraps(Long userId, Pageable pageable) {
        List<String> scrapedIds = scrapRepository.findArticleIdByUserId(userId);
        if (scrapedIds.isEmpty()) {
            return Page.empty(pageable);
        }

        return articleRepository.findByIdIn(scrapedIds, pageable)
                .map(article -> new ArticleResponse(article, true));
    }

    @Override
    @Transactional
    public void scrap(Long userId, String articleId) {
        if (!articleRepository.existsById(articleId)) {
            throw new BusinessException("존재하지 않는 기사입니다.", HttpStatus.NOT_FOUND);
        }
        if (scrapRepository.existsByUserIdAndArticleId(userId, articleId)) {
            throw new BusinessException("이미 스크랩한 기사입니다.", HttpStatus.CONFLICT);
        }
        scrapRepository.save(Scrap.builder()
                .userId(userId)
                .articleId(articleId)
                .build());
    }

    @Override
    @Transactional
    public void unscrap(Long userId, String articleId) {
        Scrap scrap = scrapRepository.findByUserIdAndArticleId(userId, articleId)
                .orElseThrow(() -> new BusinessException("스크랩하지 않은 기사입니다.", HttpStatus.NOT_FOUND));
        scrapRepository.delete(scrap);
    }
}
