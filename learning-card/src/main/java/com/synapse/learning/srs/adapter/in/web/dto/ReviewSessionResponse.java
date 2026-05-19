package com.synapse.learning.srs.adapter.in.web.dto;

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
