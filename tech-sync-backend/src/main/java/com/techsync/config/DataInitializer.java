package com.techsync.config;

import com.techsync.domain.KeywordMaster;
import com.techsync.repository.KeywordMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final KeywordMasterRepository keywordMasterRepository;

    /**
     * 표시명만으로 검색하면 의미가 모호해 엉뚱한 기사가 섞이는 키워드의 검색어 보정.
     * 예: "Spring"(봄)·"Vue"·"React" 처럼 일반 영단어/계절과 겹치는 경우.
     */
    private static final Map<String, String> SEARCH_QUERY_OVERRIDES = Map.of(
            "Spring", "Spring 프레임워크",
            "Vue", "Vue.js",
            "React", "React.js"
    );

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (keywordMasterRepository.count() == 0) {
            keywordMasterRepository.saveAll(List.of(
                // 백엔드
                build("Spring", "백엔드"),
                build("Java", "백엔드"),
                build("Node.js", "백엔드"),
                build("Docker", "백엔드"),
                build("Kubernetes", "백엔드"),
                // 프론트엔드
                build("React", "프론트엔드"),
                build("Vue", "프론트엔드"),
                build("TypeScript", "프론트엔드"),
                // AI
                build("인공지능", "AI"),
                build("ChatGPT", "AI"),
                build("LLM", "AI"),
                // 클라우드
                build("AWS", "클라우드"),
                build("클라우드", "클라우드"),
                // 보안
                build("보안", "보안"),
                build("해킹", "보안")
            ));
        }

        // 기존에 시드된 row 에도 검색어 보정을 반영한다 (searchQuery 가 비어 있을 때만).
        for (KeywordMaster km : keywordMasterRepository.findAll()) {
            String override = SEARCH_QUERY_OVERRIDES.get(km.getKeywordName());
            if (override != null && (km.getSearchQuery() == null || km.getSearchQuery().isBlank())) {
                km.applySearchQuery(override);
            }
        }
    }

    private KeywordMaster build(String name, String category) {
        return KeywordMaster.builder()
                .keywordName(name)
                .category(category)
                .searchQuery(SEARCH_QUERY_OVERRIDES.get(name))
                .build();
    }
}
