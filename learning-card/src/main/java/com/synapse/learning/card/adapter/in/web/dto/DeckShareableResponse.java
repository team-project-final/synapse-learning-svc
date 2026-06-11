package com.synapse.learning.card.adapter.in.web.dto;

public record DeckShareableResponse(
        String deckId,
        boolean shareable,
        String name,
        String description,
        String color,
        Integer cardCount,
        String reason) {
}
