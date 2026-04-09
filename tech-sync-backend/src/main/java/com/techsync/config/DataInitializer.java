package com.techsync.config;

import com.techsync.domain.KeywordMaster;
import com.techsync.repository.KeywordMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final KeywordMasterRepository keywordMasterRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (keywordMasterRepository.count() > 0) return;

        List<KeywordMaster> keywords = List.of(
            // 백엔드
            KeywordMaster.builder().keywordName("Spring").category("백엔드").build(),
            KeywordMaster.builder().keywordName("Java").category("백엔드").build(),
            KeywordMaster.builder().keywordName("Node.js").category("백엔드").build(),
            KeywordMaster.builder().keywordName("Docker").category("백엔드").build(),
            KeywordMaster.builder().keywordName("Kubernetes").category("백엔드").build(),
            // 프론트엔드
            KeywordMaster.builder().keywordName("React").category("프론트엔드").build(),
            KeywordMaster.builder().keywordName("Vue").category("프론트엔드").build(),
            KeywordMaster.builder().keywordName("TypeScript").category("프론트엔드").build(),
            // AI
            KeywordMaster.builder().keywordName("인공지능").category("AI").build(),
            KeywordMaster.builder().keywordName("ChatGPT").category("AI").build(),
            KeywordMaster.builder().keywordName("LLM").category("AI").build(),
            // 클라우드
            KeywordMaster.builder().keywordName("AWS").category("클라우드").build(),
            KeywordMaster.builder().keywordName("클라우드").category("클라우드").build(),
            // 보안
            KeywordMaster.builder().keywordName("보안").category("보안").build(),
            KeywordMaster.builder().keywordName("해킹").category("보안").build()
        );

        keywordMasterRepository.saveAll(keywords);
    }
}
