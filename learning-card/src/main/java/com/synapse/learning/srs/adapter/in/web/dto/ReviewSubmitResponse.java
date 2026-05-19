package com.synapse.learning.srs.adapter.in.web.dto;

import java.time.Instant;
import java.util.UUID;

public record ReviewSubmitResponse(
        UUID cardId,
        int rating,
        double newEaseFactor,
        int newIntervalDays,
        int lapses,
        Instant dueDate) {
}
