package com.synapse.learning.srs.api;

import java.time.Instant;
import java.util.UUID;

public record ReviewSessionResponse(
        UUID sessionId,
        UUID deckId,
        String status,
        int totalCards,
        int reviewedCards,
        Instant startedAt,
        Instant completedAt) {
}