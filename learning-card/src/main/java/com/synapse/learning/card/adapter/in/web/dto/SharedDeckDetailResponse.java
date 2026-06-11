package com.synapse.learning.card.adapter.in.web.dto;

import java.util.List;

public record SharedDeckDetailResponse(
        DeckResponse deck,
        List<CardResponse> cards) {
}
