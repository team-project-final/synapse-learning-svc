package com.synapse.learning.card.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapse.learning.card.application.DeckService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class DeckControllerTest {

    @Autowired
    WebApplicationContext context;
    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    DeckService deckService;

    MockMvc mockMvc;

    private static final String TENANT_ID = "00000000-0000-0000-0000-000000000099";
    private static final String USER_ID   = "00000000-0000-0000-0000-000000000001";
    private static final UUID   DECK_ID   = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    private DeckResponse mockResponse() {
        return new DeckResponse(DECK_ID, "테스트 덱", null, null, Instant.now(), Instant.now());
    }

    // ── POST /decks ──────────────────────────────────

    @Test
    @DisplayName("POST /decks — 덱 생성 201")
    void createDeck_returns201() throws Exception {
        given(deckService.createDeck(any(), any(), any())).willReturn(mockResponse());

        mockMvc.perform(post("/decks")
                        .header("X-User-Id", USER_ID)
                        .header("X-Tenant-Id", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new DeckCreateRequest("테스트 덱", null, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("테스트 덱"));
    }

    // ── GET /decks ───────────────────────────────────

    @Test
    @DisplayName("GET /decks — 내 덱 목록 200 (페이지네이션)")
    void getMyDecks_returns200() throws Exception {
        PageResponse<DeckResponse> page = new PageResponse<>(
                List.of(mockResponse()), 0, 20, 1, 1, true);
        given(deckService.getMyDecks(eq(USER_ID), any())).willReturn(page);

        mockMvc.perform(get("/decks")
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value("테스트 덱"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    // ── PATCH /decks/{id} ────────────────────────────

    @Test
    @DisplayName("PATCH /decks/{id} — 덱 수정 200")
    void updateDeck_returns200() throws Exception {
        given(deckService.updateDeck(any(), any(), any())).willReturn(mockResponse());

        mockMvc.perform(patch("/decks/{deckId}", DECK_ID)
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new DeckUpdateRequest("수정된 덱", null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("테스트 덱"));
    }

    @Test
    @DisplayName("PATCH /decks/{id} — 소유자 아님 403")
    void updateDeck_notOwner_returns403() throws Exception {
        willThrow(new BusinessException(ErrorCode.DECK_ACCESS_DENIED))
                .given(deckService).updateDeck(any(), any(), any());

        mockMvc.perform(patch("/decks/{deckId}", DECK_ID)
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new DeckUpdateRequest("수정", null, null))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("DECK_ACCESS_DENIED"));
    }

    // ── DELETE /decks/{id} ───────────────────────────

    @Test
    @DisplayName("DELETE /decks/{id} — 덱 삭제 204")
    void deleteDeck_returns204() throws Exception {
        mockMvc.perform(delete("/decks/{deckId}", DECK_ID)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isNoContent());
    }
}
