package com.synapse.learning.card.adapter.in.web.dto;

public record DeckCopyRequest(
        String sharedContentId,
        String shareToken) {
}
