package com.synapse.learning.card.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record DeckCreateRequest(
        @NotBlank String name,
        String description,
        String color) {
}
