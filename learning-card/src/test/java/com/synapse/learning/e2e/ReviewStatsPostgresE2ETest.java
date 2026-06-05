package com.synapse.learning.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@DisplayName("복습 통계 PostgreSQL E2E")
class ReviewStatsPostgresE2ETest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final String USER_ID = "00000000-0000-0000-0000-000000000001";
    private static final String TENANT_ID = "00000000-0000-0000-0000-000000000099";

    @Autowired
    WebApplicationContext context;

    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    MockMvc mockMvc;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @TestConfiguration
    static class CacheTestConfig {

        @Bean
        @Primary
        CacheManager postgresE2eCacheManager() {
            return new ConcurrentMapCacheManager("stats:overview", "stats:heatmap", "stats:retention");
        }
    }

    @Test
    @DisplayName("복습 완료 후 PostgreSQL 통계 API가 복습 수와 정답률을 집계한다")
    void reviewStats_복습완료후_shouldAggregateOverviewAndHeatmap() throws Exception {
        String deckId = createDeck();
        String cardId = createCard(deckId);
        String sessionId = startSession(deckId);

        submitReview(sessionId, cardId, 3);
        completeSession(sessionId);

        mockMvc.perform(get("/stats/overview")
                        .with(jwt())
                        .header("X-User-Id", USER_ID)
                        .header("X-Tenant-Id", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalReviews").value(1))
                .andExpect(jsonPath("$.data.overallCorrectRate").value(100.0))
                .andExpect(jsonPath("$.data.currentStreak").value(0))
                .andExpect(jsonPath("$.data.longestStreak").value(0));

        mockMvc.perform(get("/stats/heatmap")
                        .with(jwt())
                        .header("X-User-Id", USER_ID)
                        .header("X-Tenant-Id", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.weekly").isArray())
                .andExpect(jsonPath("$.data.weekly[11].reviewCount").value(1))
                .andExpect(jsonPath("$.data.weekly[11].correctRate").value(100.0));

        mockMvc.perform(get("/stats/retention")
                        .with(jwt())
                        .header("X-User-Id", USER_ID)
                        .header("X-Tenant-Id", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.points").isArray())
                .andExpect(jsonPath("$.data.points[29].daysAgo").value(0))
                .andExpect(jsonPath("$.data.points[29].reviewCount").value(1))
                .andExpect(jsonPath("$.data.points[29].retentionRate").value(100.0));
    }

    private String createDeck() throws Exception {
        MvcResult result = mockMvc.perform(post("/decks")
                        .with(jwt())
                        .header("X-User-Id", USER_ID)
                        .header("X-Tenant-Id", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "PostgreSQL 통계 E2E 덱"))))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asText();
    }

    private String createCard(String deckId) throws Exception {
        MvcResult result = mockMvc.perform(post("/decks/{deckId}/cards", deckId)
                        .with(jwt())
                        .header("X-User-Id", USER_ID)
                        .header("X-Tenant-Id", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("frontContent", "PostgreSQL 통계란?",
                                        "backContent", "Native query 검증",
                                        "cardType", "qa"))))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asText();
    }

    private String startSession(String deckId) throws Exception {
        MvcResult result = mockMvc.perform(post("/reviews/sessions")
                        .with(jwt())
                        .header("X-User-Id", USER_ID)
                        .header("X-Tenant-Id", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("deckId", deckId))))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("sessionId").asText();
    }

    private void submitReview(String sessionId, String cardId, int rating) throws Exception {
        mockMvc.perform(post("/reviews/sessions/{sessionId}/submit", sessionId)
                        .with(jwt())
                        .header("X-User-Id", USER_ID)
                        .header("X-Tenant-Id", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("cardId", cardId, "rating", rating, "timeSpentMs", 3000))))
                .andExpect(status().isOk());
    }

    private void completeSession(String sessionId) throws Exception {
        mockMvc.perform(put("/reviews/sessions/{sessionId}/complete", sessionId)
                        .with(jwt())
                        .header("X-Tenant-Id", TENANT_ID))
                .andExpect(status().isOk());
    }
}
