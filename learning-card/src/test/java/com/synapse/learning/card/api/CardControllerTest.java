package com.synapse.learning.card.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapse.learning.card.application.CardService;
import com.synapse.learning.shared.PageResponse;
import com.synapse.learning.shared.exception.BusinessException;
import com.synapse.learning.shared.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class CardControllerTest {

    @Autowired
    WebApplicationContext context;
    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    CardService cardService;

    MockMvc mockMvc;

    private static final String TENANT_ID = "00000000-0000-0000-0000-000000000099";
    private static final String USER_ID   = "00000000-0000-0000-0000-000000000001";
    private static final UUID   DECK_ID   = UUID.randomUUID();
    private static final UUID   CARD_ID   = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    private CardResponse mockResponse() {
        return new CardResponse(CARD_ID, DECK_ID, "qa", "스택이란?", "LIFO",
                null, "new", 2.5, 0, 0, 0, Instant.now(), null, Instant.now(), Instant.now());
    }

    // ── POST /decks/{deckId}/cards ───────────────────

    @Test
    @DisplayName("POST /decks/{deckId}/cards — 카드 생성 201")
    void createCard_returns201() throws Exception {
        given(cardService.createCard(any(), any(), any(), any())).willReturn(mockResponse());

        mockMvc.perform(post("/decks/{deckId}/cards", DECK_ID)
                        .header("X-User-Id", USER_ID)
                        .header("X-Tenant-Id", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CardCreateRequest("스택이란?", "LIFO", "qa", null, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.frontContent").value("스택이란?"));
    }

    @Test
    @DisplayName("POST /decks/{deckId}/cards — 소유자 아님 403")
    void createCard_notOwner_returns403() throws Exception {
        willThrow(new BusinessException(ErrorCode.DECK_ACCESS_DENIED))
                .given(cardService).createCard(any(), any(), any(), any());

        mockMvc.perform(post("/decks/{deckId}/cards", DECK_ID)
                        .header("X-User-Id", USER_ID)
                        .header("X-Tenant-Id", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CardCreateRequest("Q", "A", "qa", null, null))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("DECK_ACCESS_DENIED"));
    }

    // ── GET /decks/{deckId}/cards ────────────────────

    @Test
    @DisplayName("GET /decks/{deckId}/cards — 카드 목록 200 (페이지네이션)")
    void getCards_returns200() throws Exception {
        PageResponse<CardResponse> page = new PageResponse<>(
                List.of(mockResponse()), 0, 20, 1, 1, true);
        given(cardService.getCards(any(), any(), any())).willReturn(page);

        mockMvc.perform(get("/decks/{deckId}/cards", DECK_ID)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].frontContent").value("스택이란?"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    // ── PATCH /decks/{deckId}/cards/{cardId} ─────────

    @Test
    @DisplayName("PATCH /decks/{deckId}/cards/{cardId} — 카드 수정 200")
    void updateCard_returns200() throws Exception {
        given(cardService.updateCard(any(), any(), any(), any())).willReturn(mockResponse());

        mockMvc.perform(patch("/decks/{deckId}/cards/{cardId}", DECK_ID, CARD_ID)
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CardUpdateRequest("수정된 질문", "수정된 답", "qa"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.frontContent").value("스택이란?"));
    }

    // ── DELETE /decks/{deckId}/cards/{cardId} ────────

    @Test
    @DisplayName("DELETE /decks/{deckId}/cards/{cardId} — 카드 삭제 204")
    void deleteCard_returns204() throws Exception {
        mockMvc.perform(delete("/decks/{deckId}/cards/{cardId}", DECK_ID, CARD_ID)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isNoContent());
    }
}
