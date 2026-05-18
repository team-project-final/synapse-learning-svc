package com.synapse.learning.card.api;

import jakarta.validation.constraints.Size;

public record CardUpdateRequest(

        String frontContent,

        String backContent,

        @Size(max = 20) String cardType) {
}