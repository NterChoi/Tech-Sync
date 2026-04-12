package com.techsync.service;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.techsync.domain.Article;
import com.techsync.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RssCollectorService {

    private static final String GEEK_NEWS_RSS_URL = "https://feeds.feedburner.com/geeknews-feed";
    private static final String SOURCE_GEEK = "GEEK";

    private final ArticleRepository articleRepository;

    @Scheduled(initialDelay = 5000, fixedDelay = 3600000)  // 앱 시작 5초 후 첫 수집, 이후 1시간 간격
    public void collectGeekNews() {
        log.info("[RssCollector] 긱뉴스 RSS 수집 시작");

        try {
            SyndFeed feed = new SyndFeedInput().build(new XmlReader(new URL(GEEK_NEWS_RSS_URL)));
            List<SyndEntry> entries = feed.getEntries();

            List<Article> toSave = new ArrayList<>();
            for (SyndEntry entry : entries) {
                String sourceId = (entry.getUri() != null && !entry.getUri().isBlank())
                        ? entry.getUri() : entry.getLink();

                if (articleRepository.existsBySourceId(sourceId)) {
                    continue;
                }

                toSave.add(Article.builder()
                        .sourceId(sourceId)
                        .source(SOURCE_GEEK)
                        .title(entry.getTitle())
                        .link(entry.getLink())
                        .description(entry.getDescription() != null ? entry.getDescription().getValue() : null)
                        .publishedAt(toLocalDateTime(entry.getPublishedDate()))
                        .collectedAt(LocalDateTime.now())
                        .build());
            }

            articleRepository.saveAll(toSave);
            log.info("[RssCollector] 긱뉴스 RSS 수집 완료 — 저장: {}, 중복 스킵: {}",
                    toSave.size(), entries.size() - toSave.size());

        } catch (Exception e) {
            log.error("[RssCollector] 긱뉴스 RSS 수집 실패: {}", e.getMessage(), e);
        }
    }

    private LocalDateTime toLocalDateTime(Date date) {
        if (date == null) return null;
        return date.toInstant().atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime();
    }
}
