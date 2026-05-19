package com.synapse.learning.srs.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapse.learning.global.exception.BusinessException;
import com.synapse.learning.global.exception.ErrorCode;
import com.synapse.learning.srs.adapter.in.web.dto.ReviewCardResponse;
import com.synapse.learning.srs.adapter.in.web.dto.ReviewSessionResponse;
import com.synapse.learning.srs.adapter.in.web.dto.ReviewSessionStartRequest;
import com.synapse.learning.srs.adapter.in.web.dto.ReviewSessionSubmitRequest;
import com.synapse.learning.srs.adapter.in.web.dto.ReviewSubmitResponse;
import com.synapse.learning.srs.application.port.in.ReviewSessionUseCase;
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
class ReviewSessionControllerTest {

    @Autowired
    WebApplicationContext context;
    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    ReviewSessionUseCase reviewSessionUseCase;

    MockMvc mockMvc;

    private static final String TENANT_ID  = "00000000-0000-0000-0000-000000000099";
    private static final String USER_ID    = "00000000-0000-0000-0000-000000000001";
    private static final UUID   SESSION_ID = UUID.randomUUID();
    private static final UUID   DECK_ID    = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    @DisplayName("POST /reviews/sessions — 세션 시작 성공")
    void startSession_returns200() throws Exception {
        ReviewSessionResponse response = new ReviewSessionResponse(
                SESSION_ID, DECK_ID, "in_progress", 5, 0, Instant.now(), null);

        given(reviewSessionUseCase.startSession(any(), any(), any())).willReturn(response);

        mockMvc.perform(post("/reviews/sessions")
                        .header("X-User-Id", USER_ID)
                        .header("X-Tenant-Id", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReviewSessionStartRequest(DECK_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("in_progress"))
                .andExpect(jsonPath("$.data.totalCards").value(5));
    }

    @Test
    @DisplayName("GET /reviews/queue — 카드 목록 반환")
    void getReviewQueue_returns200() throws Exception {
        List<ReviewCardResponse> cards = List.of(
                new ReviewCardResponse(UUID.randomUUID(), "qa",
                        "스택이란?", "LIFO", null, 0, 2.5, Instant.now()));

        given(reviewSessionUseCase.getReviewQueue(any(), eq(DECK_ID))).willReturn(cards);

        mockMvc.perform(get("/reviews/queue")
                        .header("X-Tenant-Id", TENANT_ID)
                        .param("deckId", DECK_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].frontContent").value("스택이란?"));
    }

    @Test
    @DisplayName("PUT /reviews/sessions/{id}/complete — 세션 완료")
    void completeSession_returns200() throws Exception {
        ReviewSessionResponse response = new ReviewSessionResponse(
                SESSION_ID, DECK_ID, "completed", 5, 5, Instant.now(), Instant.now());

        given(reviewSessionUseCase.completeSession(any(), eq(SESSION_ID))).willReturn(response);

        mockMvc.perform(put("/reviews/sessions/{sessionId}/complete", SESSION_ID)
                        .header("X-Tenant-Id", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("completed"));
    }

    @Test
    @DisplayName("POST /reviews/sessions/{id}/submit — rating 제출 200")
    void submitReview_returns200() throws Exception {
        UUID cardId = UUID.randomUUID();
        ReviewSubmitResponse response = new ReviewSubmitResponse(
                cardId, 3, 2.5, 1, 0, Instant.now());

        given(reviewSessionUseCase.submitReview(any(), any(), eq(SESSION_ID), any()))
                .willReturn(response);

        mockMvc.perform(post("/reviews/sessions/{sessionId}/submit", SESSION_ID)
                        .header("X-User-Id", USER_ID)
                        .header("X-Tenant-Id", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReviewSessionSubmitRequest(cardId, 3, 3000))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rating").value(3))
                .andExpect(jsonPath("$.data.newEaseFactor").value(2.5))
                .andExpect(jsonPath("$.data.newIntervalDays").value(1));
    }

    @Test
    @DisplayName("POST /reviews/sessions/{id}/submit — 세션 없음 403")
    void submitReview_sessionNotFound_returns403() throws Exception {
        willThrow(new BusinessException(ErrorCode.DECK_ACCESS_DENIED))
                .given(reviewSessionUseCase).submitReview(any(), any(), any(), any());

        mockMvc.perform(post("/reviews/sessions/{sessionId}/submit", SESSION_ID)
                        .header("X-User-Id", USER_ID)
                        .header("X-Tenant-Id", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReviewSessionSubmitRequest(UUID.randomUUID(), 3, 1000))))
                .andExpect(status().isForbidden());
    }
}
