package com.synapse.learning.srs.application.service;

import com.synapse.learning.card.application.port.out.CardDeckPort;
import com.synapse.learning.card.application.port.out.FlashCardPort;
import com.synapse.learning.global.exception.BusinessException;
import com.synapse.learning.card.domain.model.CardDeck;
import com.synapse.learning.card.domain.model.FlashCard;
import com.synapse.learning.srs.adapter.in.web.dto.ReviewCardResponse;
import com.synapse.learning.srs.adapter.in.web.dto.ReviewSessionResponse;
import com.synapse.learning.srs.adapter.in.web.dto.ReviewSessionStartRequest;
import com.synapse.learning.srs.application.port.out.ReviewSessionPort;
import com.synapse.learning.srs.domain.model.ReviewSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReviewSessionServiceTest {

    @Mock
    ReviewSessionPort reviewSessionPort;
    @Mock
    FlashCardPort flashCardPort;
    @Mock
    CardDeckPort cardDeckPort;
    @Mock
    ReviewService reviewService;

    @InjectMocks
    ReviewSessionService reviewSessionService;

    private static final UUID TENANT_ID  = UUID.fromString("00000000-0000-0000-0000-000000000099");
    private static final UUID USER_ID    = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID DECK_ID    = UUID.randomUUID();
    private static final UUID SESSION_ID = UUID.randomUUID();

    @Test
    @DisplayName("세션 시작 시 due 카드 수만큼 totalCards가 설정된다")
    void startSession_setsTotalCards() {
        FlashCard card1 = FlashCard.builder().deckId(DECK_ID).tenantId(TENANT_ID)
                .cardType("qa").frontContent("Q1").backContent("A1").build();
        FlashCard card2 = FlashCard.builder().deckId(DECK_ID).tenantId(TENANT_ID)
                .cardType("qa").frontContent("Q2").backContent("A2").build();

        given(cardDeckPort.findByIdAndDeletedAtIsNull(DECK_ID)).willReturn(Optional.of(mockDeck()));
        given(flashCardPort.findDueCards(eq(TENANT_ID), eq(DECK_ID), any(), any(Pageable.class)))
                .willReturn(List.of(card1, card2));
        given(reviewSessionPort.save(any())).willAnswer(i -> i.getArgument(0));

        ReviewSessionResponse response = reviewSessionService.startSession(
                TENANT_ID.toString(), USER_ID.toString(),
                new ReviewSessionStartRequest(DECK_ID));

        assertThat(response.totalCards()).isEqualTo(2);
        assertThat(response.reviewedCards()).isEqualTo(0);
        assertThat(response.status()).isEqualTo("in_progress");
        verify(reviewSessionPort).save(any(ReviewSession.class));
    }

    @Test
    @DisplayName("복습 큐 조회 시 due 카드 목록이 반환된다")
    void getReviewQueue_returnsDueCards() {
        FlashCard card = FlashCard.builder().deckId(DECK_ID).tenantId(TENANT_ID)
                .cardType("qa").frontContent("스택이란?").backContent("LIFO").build();

        given(cardDeckPort.findByIdAndDeletedAtIsNull(DECK_ID)).willReturn(Optional.of(mockDeck()));
        given(flashCardPort.findDueCards(eq(TENANT_ID), eq(DECK_ID), any(), any(Pageable.class)))
                .willReturn(List.of(card));

        List<ReviewCardResponse> queue = reviewSessionService.getReviewQueue(
                TENANT_ID.toString(), USER_ID.toString(), DECK_ID);

        assertThat(queue).hasSize(1);
        assertThat(queue.get(0).frontContent()).isEqualTo("스택이란?");
        assertThat(queue.get(0).backContent()).isEqualTo("LIFO");
    }

    @Test
    @DisplayName("due 카드가 없으면 빈 큐가 반환된다")
    void getReviewQueue_empty_returnsEmptyList() {
        given(cardDeckPort.findByIdAndDeletedAtIsNull(DECK_ID)).willReturn(Optional.of(mockDeck()));
        given(flashCardPort.findDueCards(eq(TENANT_ID), eq(DECK_ID), any(), any(Pageable.class)))
                .willReturn(List.of());

        List<ReviewCardResponse> queue = reviewSessionService.getReviewQueue(
                TENANT_ID.toString(), USER_ID.toString(), DECK_ID);

        assertThat(queue).isEmpty();
    }

    @Test
    @DisplayName("세션 완료 시 status가 completed로 변경된다")
    void completeSession_setsStatusCompleted() {
        ReviewSession session = ReviewSession.builder()
                .tenantId(TENANT_ID).userId(USER_ID)
                .deckId(DECK_ID).totalCards(3).build();

        given(reviewSessionPort.findByIdAndTenantId(SESSION_ID, TENANT_ID))
                .willReturn(Optional.of(session));

        ReviewSessionResponse response = reviewSessionService.completeSession(
                TENANT_ID.toString(), USER_ID.toString(), SESSION_ID);

        assertThat(response.status()).isEqualTo("completed");
        assertThat(response.completedAt()).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 세션 완료 시 예외가 발생한다")
    void completeSession_notFound_throwsException() {
        given(reviewSessionPort.findByIdAndTenantId(SESSION_ID, TENANT_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> reviewSessionService.completeSession(
                TENANT_ID.toString(), USER_ID.toString(), SESSION_ID))
                .isInstanceOf(BusinessException.class);
    }

    private CardDeck mockDeck() {
        return CardDeck.builder()
                .tenantId(TENANT_ID)
                .userId(USER_ID)
                .name("테스트 덱")
                .build();
    }
}
