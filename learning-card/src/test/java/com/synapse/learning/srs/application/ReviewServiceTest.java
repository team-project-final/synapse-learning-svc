package com.synapse.learning.srs.application;

import com.synapse.learning.card.domain.model.FlashCard;
import com.synapse.learning.card.domain.repository.FlashCardRepository;
import com.synapse.learning.srs.api.ReviewSubmitRequest;
import com.synapse.learning.srs.api.ReviewSubmitResponse;
import com.synapse.learning.srs.domain.Sm2Calculator;
import com.synapse.learning.srs.domain.Sm2Result;
import com.synapse.learning.srs.domain.model.CardReview;
import com.synapse.learning.srs.domain.repository.CardReviewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    FlashCardRepository flashCardRepository;
    @Mock
    CardReviewRepository cardReviewRepository;
    @Mock
    Sm2Calculator sm2Calculator;

    @InjectMocks
    ReviewService reviewService;

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID CARD_ID = UUID.randomUUID();

    @Test
    @DisplayName("rating=3(GOOD) 제출 시 카드 SRS 필드가 업데이트된다")
    void submitReview_good_updatesSrsFields() {
        // given
        FlashCard card = FlashCard.builder()
                .deckId(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .cardType("qa")
                .frontContent("Q")
                .backContent("A")
                .build();

        given(flashCardRepository.findByIdAndDeletedAtIsNull(CARD_ID))
                .willReturn(Optional.of(card));
        given(sm2Calculator.calculate(3, 2.5, 0, 0))
                .willReturn(new Sm2Result(2.5, 1, 1));
        given(flashCardRepository.saveAndFlush(any())).willReturn(card);
        given(cardReviewRepository.save(any())).willReturn(null);

        // when
        ReviewSubmitResponse response = reviewService.submitReview(
                USER_ID.toString(), TENANT_ID.toString(), CARD_ID.toString(),
                new ReviewSubmitRequest(3, 1000), null);

        // then
        assertThat(response.rating()).isEqualTo(3);
        assertThat(response.newEaseFactor()).isEqualTo(2.5);
        assertThat(response.newIntervalDays()).isEqualTo(1);
        assertThat(response.lapses()).isEqualTo(0);
        verify(flashCardRepository).saveAndFlush(any());
        verify(cardReviewRepository).save(any(CardReview.class));
    }

    @Test
    @DisplayName("rating=1(AGAIN) 제출 시 lapses가 1 증가한다")
    void submitReview_again_incrementsLapses() {
        // given
        FlashCard card = FlashCard.builder()
                .deckId(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .cardType("qa")
                .frontContent("Q")
                .backContent("A")
                .build();

        given(flashCardRepository.findByIdAndDeletedAtIsNull(CARD_ID))
                .willReturn(Optional.of(card));
        given(sm2Calculator.calculate(1, 2.5, 0, 0))
                .willReturn(new Sm2Result(2.3, 0, 0));
        given(flashCardRepository.saveAndFlush(any())).willReturn(card);
        given(cardReviewRepository.save(any())).willReturn(null);

        // when
        ReviewSubmitResponse response = reviewService.submitReview(
                USER_ID.toString(), TENANT_ID.toString(), CARD_ID.toString(),
                new ReviewSubmitRequest(1, 500), null);

        // then
        assertThat(response.lapses()).isEqualTo(1);
        assertThat(response.newIntervalDays()).isEqualTo(0);
    }

    @Test
    @DisplayName("존재하지 않는 카드 ID로 제출 시 예외가 발생한다")
    void submitReview_cardNotFound_throwsException() {
        // given
        given(flashCardRepository.findByIdAndDeletedAtIsNull(CARD_ID))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reviewService.submitReview(
                USER_ID.toString(), TENANT_ID.toString(), CARD_ID.toString(),
                new ReviewSubmitRequest(3, 1000), null))
                .isInstanceOf(RuntimeException.class);
    }
}