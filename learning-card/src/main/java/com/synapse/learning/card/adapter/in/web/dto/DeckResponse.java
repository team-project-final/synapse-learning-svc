package com.synapse.learning.card.adapter.in.web.dto;

import java.time.Instant;
import java.util.UUID;

public record DeckResponse(
        UUID id,
        String name,
        String description,
        String color,
        Instant createdAt,
        Instant updatedAt) {
}
