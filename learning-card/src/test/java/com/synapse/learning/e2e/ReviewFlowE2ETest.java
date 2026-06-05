package com.synapse.learning.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@DisplayName("복습 전체 플로우 E2E")
class ReviewFlowE2ETest {

    @Autowired WebApplicationContext context;
    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    MockMvc mockMvc;

    private static final String USER_ID   = "00000000-0000-0000-0000-000000000001";
    private static final String TENANT_ID = "00000000-0000-0000-0000-000000000099";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("덱 생성 → 카드 생성 → 복습 세션 → SM-2 제출 → 세션 완료 → 통계 조회 전체 플로우가 정상 동작한다")
    void reviewFlow_카드생성부터세션완료까지_should정상동작한다() throws Exception {

        // 1. 덱 생성
        MvcResult deckResult = mockMvc.perform(post("/decks")
                        .with(jwt())
                        .header("X-User-Id", USER_ID)
                        .header("X-Tenant-Id", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "E2E 테스트 덱"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("E2E 테스트 덱"))
                .andReturn();

        String deckId = objectMapper.readTree(deckResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // 2. 카드 생성 — FlashCard.dueDate = Instant.now() 이므로 즉시 복습 대상
        MvcResult cardResult = mockMvc.perform(post("/decks/{deckId}/cards", deckId)
                        .with(jwt())
                        .header("X-User-Id", USER_ID)
                        .header("X-Tenant-Id", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("frontContent", "스택이란?",
                                        "backContent", "LIFO",
                                        "cardType", "qa"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.frontContent").value("스택이란?"))
                .andReturn();

        String cardId = objectMapper.readTree(cardResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // 3. 복습 세션 시작
        MvcResult sessionResult = mockMvc.perform(post("/reviews/sessions")
                        .with(jwt())
                        .header("X-User-Id", USER_ID)
                        .header("X-Tenant-Id", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("deckId", deckId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("in_progress"))
                .andReturn();

        String sessionId = objectMapper.readTree(sessionResult.getResponse().getContentAsString())
                .path("data").path("sessionId").asText();

        // 4. 복습 큐 조회 — 방금 만든 카드가 포함되어야 함
        mockMvc.perform(get("/reviews/queue")
                        .with(jwt())
                        .header("X-User-Id", USER_ID)
                        .header("X-Tenant-Id", TENANT_ID)
                        .param("deckId", deckId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].cardId").value(cardId));

        // 5. SM-2 rating 제출 (rating=3 GOOD → EF 유지 2.5, interval=1)
        mockMvc.perform(post("/reviews/sessions/{sessionId}/submit", sessionId)
                        .with(jwt())
                        .header("X-User-Id", USER_ID)
                        .header("X-Tenant-Id", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("cardId", cardId, "rating", 3, "timeSpentMs", 3000))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rating").value(3))
                .andExpect(jsonPath("$.data.newEaseFactor").value(2.5))
                .andExpect(jsonPath("$.data.newIntervalDays").value(1));

        // 6. 세션 완료
        mockMvc.perform(put("/reviews/sessions/{sessionId}/complete", sessionId)
                        .with(jwt())
                        .header("X-User-Id", USER_ID)
                        .header("X-Tenant-Id", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("completed"));

        // 7. 통계 API — /stats/overview는 PostgreSQL native query(DATE_TRUNC, AT TIME ZONE)를
        //    사용하므로 H2 E2E에서는 검증하지 않음 (ReviewStatsControllerTest에서 mock 커버)
    }
}
