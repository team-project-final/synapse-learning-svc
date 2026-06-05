package com.synapse.learning.srs.application.service;

import com.synapse.learning.card.application.port.out.FlashCardPort;
import com.synapse.learning.card.domain.model.FlashCard;
import com.synapse.learning.global.exception.BusinessException;
import com.synapse.learning.srs.adapter.in.web.dto.ReviewSubmitRequest;
import com.synapse.learning.srs.adapter.in.web.dto.ReviewSubmitResponse;
import com.synapse.learning.srs.application.port.out.CardReviewedEventPort;
import com.synapse.learning.srs.application.port.out.CardReviewPort;
import com.synapse.learning.srs.domain.model.CardReview;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    FlashCardPort flashCardPort;
    @Mock
    CardReviewPort cardReviewPort;
    @Mock
    CardReviewedEventPort eventPublisher;

    @InjectMocks
    ReviewService reviewService;

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    private static final UUID USER_ID   = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID CARD_ID   = UUID.randomUUID();

    @Test
    @DisplayName("rating=3(GOOD) 제출 시 카드 SRS 필드가 업데이트된다")
    void submitReview_good_updatesSrsFields() {
        FlashCard card = FlashCard.builder()
                .deckId(UUID.randomUUID()).tenantId(TENANT_ID)
                .cardType("qa").frontContent("Q").backContent("A")
                .build();

        given(flashCardPort.findByIdAndDeletedAtIsNull(CARD_ID)).willReturn(Optional.of(card));
        given(flashCardPort.existsActiveCardOwnedBy(CARD_ID, USER_ID, TENANT_ID)).willReturn(true);
        given(flashCardPort.saveAndFlush(any())).willReturn(card);
        given(cardReviewPort.save(any())).willReturn(null);

        ReviewSubmitResponse response = reviewService.submitReview(
                USER_ID.toString(), TENANT_ID.toString(), CARD_ID.toString(),
                new ReviewSubmitRequest(3, 1000), null);

        assertThat(response.rating()).isEqualTo(3);
        assertThat(response.newEaseFactor()).isEqualTo(2.5);
        assertThat(response.newIntervalDays()).isEqualTo(1);
        assertThat(response.lapses()).isEqualTo(0);
        verify(flashCardPort).saveAndFlush(any());
        verify(cardReviewPort).save(any(CardReview.class));
        verify(eventPublisher).publish(eq(USER_ID.toString()), eq(TENANT_ID.toString()), eq(CARD_ID.toString()), eq(3), any());
    }

    @Test
    @DisplayName("rating=1(AGAIN) 제출 시 lapses가 1 증가한다")
    void submitReview_again_incrementsLapses() {
        FlashCard card = FlashCard.builder()
                .deckId(UUID.randomUUID()).tenantId(TENANT_ID)
                .cardType("qa").frontContent("Q").backContent("A")
                .build();

        given(flashCardPort.findByIdAndDeletedAtIsNull(CARD_ID)).willReturn(Optional.of(card));
        given(flashCardPort.existsActiveCardOwnedBy(CARD_ID, USER_ID, TENANT_ID)).willReturn(true);
        given(flashCardPort.saveAndFlush(any())).willReturn(card);
        given(cardReviewPort.save(any())).willReturn(null);

        ReviewSubmitResponse response = reviewService.submitReview(
                USER_ID.toString(), TENANT_ID.toString(), CARD_ID.toString(),
                new ReviewSubmitRequest(1, 500), null);

        assertThat(response.lapses()).isEqualTo(1);
        assertThat(response.newIntervalDays()).isEqualTo(1);
    }

    @Test
    @DisplayName("존재하지 않는 카드 ID로 제출 시 예외가 발생한다")
    void submitReview_cardNotFound_throwsException() {
        given(flashCardPort.findByIdAndDeletedAtIsNull(CARD_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.submitReview(
                USER_ID.toString(), TENANT_ID.toString(), CARD_ID.toString(),
                new ReviewSubmitRequest(3, 1000), null))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("submitReview denies cards not owned by current user")
    void submitReview_notOwnedCard_throwsException() {
        FlashCard card = FlashCard.builder()
                .deckId(UUID.randomUUID()).tenantId(TENANT_ID)
                .cardType("qa").frontContent("Q").backContent("A")
                .build();

        given(flashCardPort.findByIdAndDeletedAtIsNull(CARD_ID)).willReturn(Optional.of(card));
        given(flashCardPort.existsActiveCardOwnedBy(CARD_ID, USER_ID, TENANT_ID)).willReturn(false);

        assertThatThrownBy(() -> reviewService.submitReview(
                USER_ID.toString(), TENANT_ID.toString(), CARD_ID.toString(),
                new ReviewSubmitRequest(3, 1000), null))
                .isInstanceOf(BusinessException.class);

        verify(flashCardPort, never()).saveAndFlush(any());
        verify(cardReviewPort, never()).save(any());
        verify(eventPublisher, never()).publish(any(), any(), any(), anyInt(), any());
    }
}
