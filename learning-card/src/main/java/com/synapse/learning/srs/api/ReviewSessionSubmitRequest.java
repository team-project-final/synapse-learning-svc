package com.synapse.learning.srs.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ReviewSessionSubmitRequest(
        @NotNull UUID cardId,
        @NotNull @Min(1) @Max(4) Integer rating,
        Integer timeSpentMs) {
}