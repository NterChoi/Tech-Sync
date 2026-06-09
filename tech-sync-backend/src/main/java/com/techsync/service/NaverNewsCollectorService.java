package com.techsync.service;

import com.techsync.domain.Article;
import com.techsync.domain.Keyword;
import com.techsync.domain.KeywordMaster;
import com.techsync.repository.ArticleRepository;
import com.techsync.repository.KeywordMasterRepository;
import com.techsync.repository.KeywordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.HtmlUtils;
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
    private final KeywordRepository keywordRepository;
    private final AlarmService alarmService;
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
            // 검색은 보정된 질의어(getEffectiveQuery)로, 저장/구독매칭은 표시명(keywordName)으로 한다.
            int saved = collectByKeyword(keyword.getKeywordName(), keyword.getEffectiveQuery());
            totalSaved += saved;
        }

        log.info("[NaverCollector] 네이버 뉴스 수집 완료 — 총 저장: {}건", totalSaved);
    }

    private int collectByKeyword(String keyword, String searchQuery) {
        try {
            URI uri = UriComponentsBuilder.fromUriString(naverNewsUrl)
                    .queryParam("query", searchQuery)
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

            if (!toSave.isEmpty()) {
                notifySubscribers(keyword, toSave.size());
            }
            return toSave.size();

        } catch (Exception e) {
            log.error("[NaverCollector] 키워드 '{}' 수집 실패: {}", keyword, e.getMessage(), e);
            return 0;
        }
    }

    /** 해당 키워드 구독자에게 새 뉴스 도착 알림을 발행한다 (키워드당 1건, 건수 집계). */
    private void notifySubscribers(String keyword, int newCount) {
        List<Keyword> subscribers = keywordRepository.findByKeywordName(keyword);
        if (subscribers.isEmpty()) return;

        String message = String.format("구독하신 '%s' 키워드의 새 뉴스 %d건이 도착했어요.", keyword, newCount);
        for (Keyword sub : subscribers) {
            try {
                alarmService.notify(sub.getUserId(), "KEYWORD_NEWS", message, null);
            } catch (Exception e) {
                log.warn("[NaverCollector] 키워드 '{}' 알림 발행 실패 (userId={}): {}",
                        keyword, sub.getUserId(), e.getMessage());
            }
        }
        log.debug("[NaverCollector] 키워드 '{}' 알림 발행 — 구독자 {}명", keyword, subscribers.size());
    }

    private LocalDateTime parseNaverDate(String pubDate) {
        if (pubDate == null) return null;
        return ZonedDateTime.parse(pubDate, NAVER_DATE_FORMAT).toLocalDateTime();
    }

    /** 네이버 응답의 &lt;b&gt; 태그 제거 + HTML 엔티티(&amp;quot; 등) 디코딩. */
    private String stripHtmlTags(String text) {
        if (text == null) return null;
        String withoutTags = text.replaceAll("<[^>]*>", "");
        return HtmlUtils.htmlUnescape(withoutTags);
    }
}
