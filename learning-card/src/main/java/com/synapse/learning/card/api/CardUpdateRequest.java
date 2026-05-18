package com.synapse.learning.card.api;

import jakarta.validation.constraints.Size;

public record CardUpdateRequest(

        String front,

        String back,

        @Size(max = 20) String cardType) {
}