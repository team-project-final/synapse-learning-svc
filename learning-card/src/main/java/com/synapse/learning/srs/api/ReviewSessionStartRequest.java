package com.synapse.learning.srs.api;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ReviewSessionStartRequest(
        @NotNull UUID deckId) {
}