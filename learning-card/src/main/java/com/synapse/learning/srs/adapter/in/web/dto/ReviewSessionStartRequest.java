package com.synapse.learning.srs.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ReviewSessionStartRequest(
        @NotNull UUID deckId) {
}
