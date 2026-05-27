package com.synapse.learning.card.adapter.in.web.dto;

import java.time.Instant;
import java.util.UUID;

public record CardResponse(
        UUID id,
        UUID deckId,
        String cardType,
        String frontContent,
        String backContent,
        String bloomLevel,
        String status,
        double easinessFactor,
        int intervalDays,
        int repetitions,
        int lapses,
        Instant dueDate,
        Instant lastReviewedAt,
        Instant createdAt,
        Instant updatedAt) {
}
