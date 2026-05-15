package com.synapse.learning.card.application;

import com.synapse.learning.card.api.DeckResponse;
import com.synapse.learning.card.domain.model.CardDeck;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CardDeckMapper {

    // Entity → Response DTO 변환만 사용
    DeckResponse toResponse(CardDeck cardDeck);
}