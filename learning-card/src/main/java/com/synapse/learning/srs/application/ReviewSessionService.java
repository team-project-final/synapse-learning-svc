package com.synapse.learning.srs.application;

import com.synapse.learning.card.domain.model.FlashCard;
import com.synapse.learning.card.domain.repository.FlashCardRepository;
import com.synapse.learning.srs.api.*;
import com.synapse.learning.srs.domain.model.ReviewSession;
import com.synapse.learning.srs.domain.repository.ReviewSessionRepository;
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
public class ReviewSessionService {

    private final ReviewSessionRepository reviewSessionRepository;
    private final FlashCardRepository flashCardRepository;
    private final ReviewService reviewService;

    @Transactional
    public ReviewSessionResponse startSession(String tenantId, String userId,
            ReviewSessionStartRequest request) {
        List<FlashCard> dueCards = flashCardRepository.findDueCards(
                request.deckId(), Instant.now(), PageRequest.of(0, 50));

        ReviewSession session = ReviewSession.builder()
                .tenantId(UUID.fromString(tenantId))
                .userId(UUID.fromString(userId))
                .deckId(request.deckId())
                .totalCards(dueCards.size())
                .build();

        reviewSessionRepository.save(session);
        return toResponse(session);
    }

    public List<ReviewCardResponse> getReviewQueue(String tenantId, UUID deckId) {
        List<FlashCard> cards = flashCardRepository.findDueCards(
                deckId, Instant.now(), PageRequest.of(0, 50));

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

    @Transactional
    public ReviewSubmitResponse submitReview(String tenantId, String userId,
            UUID sessionId, ReviewSessionSubmitRequest request) {
        ReviewSession session = reviewSessionRepository
                .findByIdAndTenantId(sessionId, UUID.fromString(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        ReviewSubmitRequest submitRequest = new ReviewSubmitRequest(
                request.rating(), request.timeSpentMs());

        ReviewSubmitResponse response = reviewService.submitReview(
                userId, tenantId, request.cardId().toString(), submitRequest, sessionId);

        session.incrementReviewed();
        return response;
    }

    @Transactional
    public ReviewSessionResponse completeSession(String tenantId, UUID sessionId) {
        ReviewSession session = reviewSessionRepository
                .findByIdAndTenantId(sessionId, UUID.fromString(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

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
}