package com.techsync.service;

import com.techsync.domain.Article;
import com.techsync.domain.KeywordMaster;
import com.techsync.repository.ArticleRepository;
import com.techsync.repository.KeywordMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NaverNewsCollectorService {

    private static final String SOURCE_NAVER = "NAVER";
    private static final DateTimeFormatter NAVER_DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", java.util.Locale.ENGLISH);

    @Value("${naver.news.client-id}")
    private String clientId;

    @Value("${naver.news.client-secret}")
    private String clientSecret;

    @Value("${naver.news.url}")
    private String naverNewsUrl;

    private final ArticleRepository articleRepository;
    private final KeywordMasterRepository keywordMasterRepository;
    private final RestTemplate restTemplate;

    @Scheduled(initialDelay = 10000, fixedDelay = 3600000)  // 앱 시작 10초 후 첫 수집, 이후 1시간 간격
    public void collectNaverNews() {
        List<KeywordMaster> keywords = keywordMasterRepository.findAll();
        if (keywords.isEmpty()) {
            log.info("[NaverCollector] 수집할 키워드 없음");
            return;
        }

        log.info("[NaverCollector] 네이버 뉴스 수집 시작 — 키워드 {}개", keywords.size());
        int totalSaved = 0;

        for (KeywordMaster keyword : keywords) {
            int saved = collectByKeyword(keyword.getKeywordName());
            totalSaved += saved;
        }

        log.info("[NaverCollector] 네이버 뉴스 수집 완료 — 총 저장: {}건", totalSaved);
    }

    private int collectByKeyword(String keyword) {
        try {
            URI uri = UriComponentsBuilder.fromUriString(naverNewsUrl)
                    .queryParam("query", keyword)
                    .queryParam("display", 100)
                    .queryParam("sort", "date")
                    .build()
                    .encode()
                    .toUri();

            RequestEntity<Void> request = RequestEntity.get(uri)
                    .header("X-Naver-Client-Id", clientId)
                    .header("X-Naver-Client-Secret", clientSecret)
                    .build();

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    request, new ParameterizedTypeReference<Map<String, Object>>() {});

            Map<String, Object> body = response.getBody();
            if (body == null) return 0;

            List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
            if (items == null || items.isEmpty()) {
                return 0;
            }

            List<Article> toSave = new ArrayList<>();
            for (Map<String, Object> item : items) {
                String link = (String) item.get("link");
                if (articleRepository.existsBySourceId(link)) {
                    continue;
                }

                toSave.add(Article.builder()
                        .sourceId(link)
                        .source(SOURCE_NAVER)
                        .title(stripHtmlTags((String) item.get("title")))
                        .link(link)
                        .description(stripHtmlTags((String) item.get("description")))
                        .keyword(keyword)
                        .publishedAt(parseNaverDate((String) item.get("pubDate")))
                        .collectedAt(LocalDateTime.now())
                        .build());
            }

            articleRepository.saveAll(toSave);
            log.debug("[NaverCollector] 키워드 '{}' — 저장: {}건, 중복 스킵: {}건",
                    keyword, toSave.size(), items.size() - toSave.size());
            return toSave.size();

        } catch (Exception e) {
            log.error("[NaverCollector] 키워드 '{}' 수집 실패: {}", keyword, e.getMessage(), e);
            return 0;
        }
    }

    private LocalDateTime parseNaverDate(String pubDate) {
        if (pubDate == null) return null;
        return ZonedDateTime.parse(pubDate, NAVER_DATE_FORMAT).toLocalDateTime();
    }

    private String stripHtmlTags(String text) {
        if (text == null) return null;
        return text.replaceAll("<[^>]*>", "");
    }
}
