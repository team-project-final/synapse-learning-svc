package com.synapse.learning.card.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CardUpdateRequest(
        @NotBlank String frontContent,
        @NotBlank String backContent,
        String cardType) {
}
