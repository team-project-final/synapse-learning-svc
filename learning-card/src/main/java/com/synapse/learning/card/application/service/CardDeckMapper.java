package com.synapse.learning.card.application.service;

import com.synapse.learning.card.adapter.in.web.dto.DeckResponse;
import com.synapse.learning.card.domain.model.CardDeck;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CardDeckMapper {

    DeckResponse toResponse(CardDeck cardDeck);
}
