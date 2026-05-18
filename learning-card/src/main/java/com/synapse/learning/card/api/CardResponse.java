package com.synapse.learning.card.api;

import java.time.Instant;
import java.util.UUID;

public record CardResponse(
        UUID id,
        UUID deckId,
        String cardType,
        String front,
        String back,
        String bloomLevel,
        String state,
        Instant nextReviewAt,
        Instant createdAt,
        Instant updatedAt) {
}