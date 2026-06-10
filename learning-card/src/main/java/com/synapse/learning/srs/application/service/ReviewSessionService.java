package com.synapse.learning.srs.application.service;

import com.synapse.learning.card.application.port.out.FlashCardPort;
import com.synapse.learning.card.application.port.out.CardDeckPort;
import com.synapse.learning.card.domain.exception.DeckNotFoundException;
import com.synapse.learning.card.domain.model.CardDeck;
import com.synapse.learning.card.domain.model.FlashCard;
import com.synapse.learning.global.exception.BusinessException;
import com.synapse.learning.global.exception.ErrorCode;
import com.synapse.learning.srs.adapter.in.web.dto.ReviewCardResponse;
import com.synapse.learning.srs.adapter.in.web.dto.ReviewSessionResponse;
import com.synapse.learning.srs.adapter.in.web.dto.ReviewSessionStartRequest;
import com.synapse.learning.srs.adapter.in.web.dto.ReviewSessionSubmitRequest;
import com.synapse.learning.srs.adapter.in.web.dto.ReviewSubmitRequest;
import com.synapse.learning.srs.adapter.in.web.dto.ReviewSubmitResponse;
import com.synapse.learning.srs.application.port.in.ReviewSessionUseCase;
import com.synapse.learning.srs.application.port.out.ReviewSessionPort;
import com.synapse.learning.srs.domain.model.ReviewSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewSessionService implements ReviewSessionUseCase {

    private final ReviewSessionPort reviewSessionPort;
    private final FlashCardPort flashCardPort;
    private final CardDeckPort cardDeckPort;
    private final ReviewService reviewService;

    @Override
    @Transactional
    public ReviewSessionResponse startSession(String tenantId, String userId,
            ReviewSessionStartRequest request) {
        validateDeckAccess(request.deckId(), userId, tenantId);
        List<FlashCard> dueCards = flashCardPort.findDueCards(
                UUID.fromString(tenantId), request.deckId(), Instant.now(), PageRequest.of(0, 50));

        ReviewSession session = ReviewSession.builder()
                .tenantId(UUID.fromString(tenantId))
                .userId(UUID.fromString(userId))
                .deckId(request.deckId())
                .totalCards(dueCards.size())
                .build();

        reviewSessionPort.save(session);
        return toResponse(session);
    }

    @Override
    public List<ReviewCardResponse> getReviewQueue(String tenantId, String userId, UUID deckId) {
        validateDeckAccess(deckId, userId, tenantId);
        List<FlashCard> cards = flashCardPort.findDueCards(
                UUID.fromString(tenantId), deckId, Instant.now(), PageRequest.of(0, 50));

        return cards.stream()
                .map(c -> new ReviewCardResponse(
                        c.getId(),
                        c.getCardType(),
                        c.getFrontContent(),
                        c.getBackContent(),
                        c.getBloomLevel(),
                        c.getRepetitions(),
                        c.getEasinessFactor(),
                        c.getDueDate()))
                .toList();
    }

    @Override
    @Transactional
    public ReviewSubmitResponse submitReview(String tenantId, String userId,
            UUID sessionId, ReviewSessionSubmitRequest request) {
        ReviewSession session = reviewSessionPort
                .findByIdAndTenantId(sessionId, UUID.fromString(tenantId))
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
        if (!session.getUserId().equals(UUID.fromString(userId))) {
            throw new BusinessException(ErrorCode.SESSION_ACCESS_DENIED);
        }

        ReviewSubmitRequest submitRequest = new ReviewSubmitRequest(
                request.rating(), request.timeSpentMs());

        ReviewSubmitResponse response = reviewService.submitReview(
                userId, tenantId, request.cardId().toString(), submitRequest, sessionId);

        session.incrementReviewed();
        return response;
    }

    @Override
    @Transactional
    public ReviewSessionResponse completeSession(String tenantId, String userId, UUID sessionId) {
        ReviewSession session = reviewSessionPort
                .findByIdAndTenantId(sessionId, UUID.fromString(tenantId))
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
        if (!session.getUserId().equals(UUID.fromString(userId))) {
            throw new BusinessException(ErrorCode.SESSION_ACCESS_DENIED);
        }

        session.complete();
        return toResponse(session);
    }

    private ReviewSessionResponse toResponse(ReviewSession session) {
        return new ReviewSessionResponse(
                session.getId(),
                session.getDeckId(),
                session.getStatus(),
                session.getTotalCards(),
                session.getReviewedCards(),
                session.getStartedAt(),
                session.getCompletedAt());
    }

    private void validateDeckAccess(UUID deckId, String userId, String tenantId) {
        CardDeck deck = cardDeckPort.findByIdAndDeletedAtIsNull(deckId)
                .orElseThrow(() -> new DeckNotFoundException(deckId.toString()));
        if (!deck.getTenantId().equals(UUID.fromString(tenantId))
                || !deck.getUserId().equals(UUID.fromString(userId))) {
            throw new BusinessException(ErrorCode.DECK_ACCESS_DENIED);
        }
    }
}
