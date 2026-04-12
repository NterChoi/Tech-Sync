package com.techsync;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("local")
@TestPropertySource(properties = {
        "naver.news.client-id=test",
        "naver.news.client-secret=test",
        "naver.news.url=https://openapi.naver.com/v1/search/news.json"
})
class TechSyncApplicationTests {

    @Test
    void contextLoads() {
    }
}
