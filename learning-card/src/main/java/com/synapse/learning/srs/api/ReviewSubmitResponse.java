package com.synapse.learning.srs.api;

import java.time.Instant;
import java.util.UUID;

public record ReviewSubmitResponse(
        UUID cardId,
        int rating,
        double newEaseFactor,
        int newIntervalDays,
        int repetitions,
        Instant nextReviewAt) {
}