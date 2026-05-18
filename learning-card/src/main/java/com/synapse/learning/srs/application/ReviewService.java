package com.synapse.learning.srs.application;

import com.synapse.learning.card.domain.exception.CardNotFoundException;
import com.synapse.learning.card.domain.model.FlashCard;
import com.synapse.learning.card.domain.repository.FlashCardRepository;
import com.synapse.learning.shared.exception.BusinessException;
import com.synapse.learning.shared.exception.ErrorCode;
import com.synapse.learning.srs.api.ReviewSubmitRequest;
import com.synapse.learning.srs.api.ReviewSubmitResponse;
import com.synapse.learning.srs.domain.Sm2Calculator;
import com.synapse.learning.srs.domain.Sm2Result;
import com.synapse.learning.srs.domain.model.CardReview;
import com.synapse.learning.srs.domain.repository.CardReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final FlashCardRepository flashCardRepository;
    private final CardReviewRepository cardReviewRepository;
    private final Sm2Calculator sm2Calculator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public ReviewSubmitResponse submitReview(String userId, String tenantId,
            String cardId, ReviewSubmitRequest request) {
        // 카드 조회
        FlashCard card = flashCardRepository
                .findByIdAndDeletedAtIsNull(UUID.fromString(cardId))
                .orElseThrow(() -> new CardNotFoundException(cardId));

        // 카드 소유자 검증 (덱을 통해)
        // TODO: JWT 도입 시 실제 소유자 검증으로 교체

        // 현재 SRS 상태 파싱
        double prevEF = parseSrsStateEF(card.getSrsState());
        int prevInterval = (int) ChronoUnit.DAYS.between(
                card.getCreatedAt(), card.getNextReviewAt());
        if (prevInterval < 0)
            prevInterval = 0;
        int prevRepetitions = parseSrsStateRepetitions(card.getSrsState());

        // SM-2 계산
        Sm2Result result = sm2Calculator.calculate(
                request.rating(), prevEF, prevInterval, prevRepetitions);

        // 카드 SRS 상태 업데이트
        Instant nextReviewAt = Instant.now().plus(result.intervalDays(), ChronoUnit.DAYS);
        updateCardSrsState(card, result, nextReviewAt);
        flashCardRepository.saveAndFlush(card);

        // 복습 이력 저장
        CardReview review = CardReview.builder()
                .tenantId(tenantId)
                .cardId(UUID.fromString(cardId))
                .rating(request.rating())
                .prevEaseFactor(prevEF)
                .newEaseFactor(result.easeFactor())
                .prevInterval(prevInterval)
                .newInterval(result.intervalDays())
                .repetitions(result.repetitions())
                .build();
        cardReviewRepository.save(review);

        return new ReviewSubmitResponse(
                UUID.fromString(cardId),
                request.rating(),
                result.easeFactor(),
                result.intervalDays(),
                result.repetitions(),
                nextReviewAt);
    }

    // ── 내부 헬퍼 ──────────────────────────────────
    private double parseSrsStateEF(String srsState) {
        try {
            Map<String, Object> map = objectMapper.readValue(srsState, new TypeReference<>() {
            });
            return ((Number) map.get("easeFactor")).doubleValue();
        } catch (Exception e) {
            return 2.5;
        }
    }

    private int parseSrsStateRepetitions(String srsState) {
        try {
            Map<String, Object> map = objectMapper.readValue(srsState, new TypeReference<>() {
            });
            return ((Number) map.get("repetitions")).intValue();
        } catch (Exception e) {
            return 0;
        }
    }

    private void updateCardSrsState(FlashCard card, Sm2Result result, Instant nextReviewAt) {
        String newSrsState = String.format(
                "{\"easeFactor\":%.2f,\"intervalDays\":%d,\"repetitions\":%d,\"lapses\":0}",
                result.easeFactor(), result.intervalDays(), result.repetitions());
        card.updateSrsState(newSrsState, nextReviewAt, result.repetitions());
    }
}