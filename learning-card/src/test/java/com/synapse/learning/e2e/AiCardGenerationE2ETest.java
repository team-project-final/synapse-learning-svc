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

import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AI 카드 자동 생성 E2E — learning-ai → learning-card HTTP 호출 시뮬레이션
 *
 * 실제 플로우:
 *   note.created (Kafka) → learning-ai (AI 카드 생성) → POST /decks/{deckId}/cards → learning-card DB 저장
 *
 * 이 테스트에서 learning-ai 역할을 MockMvc 호출로 대체하여 learning-card 파트를 검증한다.
 *
 * 시나리오 1: AI 카드 개별 저장 (learning-ai card_client.py 호출 패턴)
 * 시나리오 2: AI 카드 배치 저장 (POST /batch)
 * 시나리오 3: AI 카드가 복습 큐에 즉시 포함됨 (dueDate = Instant.now())
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@DisplayName("[E2E] AI 카드 자동 생성 — note.created → 덱 저장")
class AiCardGenerationE2ETest {

    private static final String USER_ID   = "00000000-0000-0000-0000-000000000002";
    private static final String TENANT_ID = "00000000-0000-0000-0000-000000000099";
    private static final String NOTE_ID   = "00000000-0000-0000-0000-000000000abc";

    @Autowired
    WebApplicationContext context;

    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    // ── 시나리오 1: AI 카드 개별 저장 ──

    @Test
    @DisplayName("learning-ai가 POST /decks/{deckId}/cards 를 3회 호출하면 AI_GENERATED 카드 3개가 덱에 저장된다")
    void aiCard_개별저장_3개_모두_저장된다() throws Exception {
        String deckId = createDeck("AI 카드 E2E 덱");

        List<Map<String, Object>> aiCards = List.of(
                Map.of("frontContent", "스택(Stack)이란?",
                        "backContent", "LIFO(Last In First Out) 구조의 자료구조",
                        "cardType", "AI_GENERATED",
                        "sourceId", NOTE_ID),
                Map.of("frontContent", "큐(Queue)란?",
                        "backContent", "FIFO(First In First Out) 구조의 자료구조",
                        "cardType", "AI_GENERATED",
                        "sourceId", NOTE_ID),
                Map.of("frontContent", "힙(Heap)이란?",
                        "backContent", "완전 이진 트리 기반의 우선순위 큐",
                        "cardType", "AI_GENERATED",
                        "sourceId", NOTE_ID)
        );

        for (Map<String, Object> card : aiCards) {
            mockMvc.perform(post("/decks/{deckId}/cards", deckId)
                            .with(jwt())
                            .header("X-User-Id", USER_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(card)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.cardType").value("AI_GENERATED"));
        }

        // 덱 카드 목록 조회 — 3개 모두 존재해야 함
        mockMvc.perform(get("/decks/{deckId}/cards", deckId)
                        .with(jwt())
                        .header("X-User-Id", USER_ID)
                        .header("X-Tenant-Id", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(3))
                .andExpect(jsonPath("$.data.content[0].cardType").value("AI_GENERATED"))
                .andExpect(jsonPath("$.data.content[1].cardType").value("AI_GENERATED"))
                .andExpect(jsonPath("$.data.content[2].cardType").value("AI_GENERATED"));
    }

    // ── 시나리오 2: AI 카드 배치 저장 ──

    @Test
    @DisplayName("POST /decks/{deckId}/cards/batch 로 AI 카드를 한 번에 저장할 수 있다")
    void aiCard_배치저장_한번에_저장된다() throws Exception {
        String deckId = createDeck("AI 배치 E2E 덱");

        List<Map<String, Object>> cards = List.of(
                Map.of("frontContent", "OSI 7계층이란?",
                        "backContent", "네트워크 통신을 7단계로 나눈 표준 모델",
                        "cardType", "AI_GENERATED",
                        "sourceId", NOTE_ID),
                Map.of("frontContent", "TCP와 UDP의 차이는?",
                        "backContent", "TCP는 신뢰성 보장, UDP는 속도 우선",
                        "cardType", "AI_GENERATED",
                        "sourceId", NOTE_ID)
        );

        mockMvc.perform(post("/decks/{deckId}/cards/batch", deckId)
                        .with(jwt())
                        .header("X-User-Id", USER_ID)
                        .header("X-Tenant-Id", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("cards", cards))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].cardType").value("AI_GENERATED"))
                .andExpect(jsonPath("$.data[1].cardType").value("AI_GENERATED"));
    }

    // ── 시나리오 3: AI 카드가 즉시 복습 대상이 됨 ──

    @Test
    @DisplayName("저장된 AI 카드는 즉시 복습 큐에 포함된다 (dueDate = 생성 즉시)")
    void aiCard_저장후_즉시_복습큐에_포함된다() throws Exception {
        String deckId = createDeck("AI 복습큐 E2E 덱");

        // AI 카드 1개 저장
        MvcResult cardResult = mockMvc.perform(post("/decks/{deckId}/cards", deckId)
                        .with(jwt())
                        .header("X-User-Id", USER_ID)
                        .header("X-Tenant-Id", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "frontContent", "그래프(Graph)란?",
                                "backContent", "정점(V)과 간선(E)으로 이루어진 자료구조",
                                "cardType", "AI_GENERATED",
                                "sourceId", NOTE_ID))))
                .andExpect(status().isCreated())
                .andReturn();

        String cardId = objectMapper.readTree(cardResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // 복습 큐 조회 — 방금 저장한 AI 카드가 포함되어야 함
        mockMvc.perform(get("/reviews/queue")
                        .with(jwt())
                        .header("X-User-Id", USER_ID)
                        .header("X-Tenant-Id", TENANT_ID)
                        .param("deckId", deckId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].cardId").value(cardId));
    }

    // ── 헬퍼 ──

    private String createDeck(String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/decks")
                        .with(jwt())
                        .header("X-User-Id", USER_ID)
                        .header("X-Tenant-Id", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", name))))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asText();
    }
}
