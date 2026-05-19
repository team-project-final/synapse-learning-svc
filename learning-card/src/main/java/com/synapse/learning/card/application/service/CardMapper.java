package com.synapse.learning.card.application.service;

import com.synapse.learning.card.adapter.in.web.dto.CardResponse;
import com.synapse.learning.card.domain.model.FlashCard;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CardMapper {

    CardResponse toResponse(FlashCard flashCard);
}
