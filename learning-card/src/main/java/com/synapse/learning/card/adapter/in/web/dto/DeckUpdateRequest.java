package com.synapse.learning.card.adapter.in.web.dto;

public record DeckUpdateRequest(
        String name,
        String description,
        String color) {
}
