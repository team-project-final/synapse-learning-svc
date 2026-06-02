package com.synapse.learning.card.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeckCreateRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 1000) String description,
        @Size(max = 50) String color) {
}
