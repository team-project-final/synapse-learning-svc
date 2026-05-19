package com.synapse.learning.srs.api;

import java.time.Instant;
import java.util.UUID;

public record ReviewCardResponse(
        UUID cardId,
        String cardType,
        String frontContent,
        String backContent,
        String bloomLevel,
        int repetitions,
        double easinessFactor,
        Instant dueDate) {
}