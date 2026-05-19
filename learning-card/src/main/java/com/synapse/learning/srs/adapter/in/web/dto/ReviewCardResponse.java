package com.synapse.learning.srs.adapter.in.web.dto;

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
