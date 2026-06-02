package com.synapse.learning.card.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CardCreateRequest(
        @NotBlank @Size(max = 5000) String frontContent,
        @NotBlank @Size(max = 5000) String backContent,
        @NotBlank String cardType,
        String sourceId,
        String bloomLevel) {
}
