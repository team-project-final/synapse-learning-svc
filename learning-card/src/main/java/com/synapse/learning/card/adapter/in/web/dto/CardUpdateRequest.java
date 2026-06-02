package com.synapse.learning.card.adapter.in.web.dto;

import jakarta.validation.constraints.Size;

public record CardUpdateRequest(
        @Size(max = 5000) String frontContent,
        @Size(max = 5000) String backContent,
        String cardType) {
}
