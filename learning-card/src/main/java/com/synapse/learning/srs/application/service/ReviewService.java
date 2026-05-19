package com.synapse.learning.srs.application.service;

import com.synapse.learning.card.application.port.out.FlashCardPort;
import com.synapse.learning.card.domain.exception.CardNotFoundException;
import com.synapse.learning.card.domain.model.FlashCard;
import com.synapse.learning.srs.adapter.in.web.dto.ReviewSubmitRequest;
import com.synapse.learning.srs.adapter.in.web.dto.ReviewSubmitResponse;
import com.synapse.learning.srs.application.port.out.CardReviewPort;
import com.synapse.learning.srs.domain.Sm2Calculator;
import com.synapse.learning.srs.domain.Sm2Result;
import com.synapse.learning.srs.domain.model.CardReview;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final FlashCardPort flashCardPort;
    private final CardReviewPort cardReviewPort;
    private final Sm2Calculator sm2Calculator;

    @Transactional
    public ReviewSubmitResponse submitReview(String userId, String tenantId,
            String cardId, ReviewSubmitRequest request, UUID sessionId) {
        // 카드 조회
        FlashCard card = flashCardPort
                .findByIdAndDeletedAtIsNull(UUID.fromString(cardId))
                .orElseThrow(() -> new CardNotFoundException(cardId));

        // TODO: JWT 도입 시 실제 소유자 검증으로 교체

        double prevEF = card.getEasinessFactor();
        int prevInterval = card.getIntervalDays();
        int prevLapses = card.getLapses();

        // SM-2 계산
        Sm2Result result = sm2Calculator.calculate(
                request.rating(), prevEF, prevInterval, card.getRepetitions());

        // dueDate 계산 (interval=0 → 10분 후, 그 외 → N일 후)
        Instant dueDate = result.intervalDays() == 0
                ? Instant.now().plus(10, ChronoUnit.MINUTES)
                : Instant.now().plus(result.intervalDays(), ChronoUnit.DAYS);

        // lapses: Again(1)이면 +1
        int newLapses = (request.rating() == 1) ? prevLapses + 1 : prevLapses;

        // 카드 SRS 필드 업데이트
        card.updateSrsFields(result.easeFactor(), result.intervalDays(),
                result.repetitions(), newLapses, dueDate);
        flashCardPort.saveAndFlush(card);

        // 복습 이력 저장
        cardReviewPort.save(CardReview.builder()
                .tenantId(UUID.fromString(tenantId))
                .cardId(UUID.fromString(cardId))
                .rating(request.rating())
                .prevEaseFactor(prevEF)
                .newEaseFactor(result.easeFactor())
                .prevInterval(prevInterval)
                .newInterval(result.intervalDays())
                .timeSpentMs(request.timeSpentMs())
                .sessionId(sessionId)
                .build());

        return new ReviewSubmitResponse(
                UUID.fromString(cardId),
                request.rating(),
                result.easeFactor(),
                result.intervalDays(),
                newLapses,
                dueDate);
    }
}
