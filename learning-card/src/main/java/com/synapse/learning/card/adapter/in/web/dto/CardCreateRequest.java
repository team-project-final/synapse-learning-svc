package com.synapse.learning.card.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CardCreateRequest(
        @NotBlank String frontContent,
        @NotBlank String backContent,
        @NotNull String cardType,
        String sourceId,
        String bloomLevel) {
}
