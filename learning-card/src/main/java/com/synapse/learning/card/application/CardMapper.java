package com.synapse.learning.card.application;

import com.synapse.learning.card.api.CardResponse;
import com.synapse.learning.card.domain.model.FlashCard;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CardMapper {

    CardResponse toResponse(FlashCard flashCard);
}