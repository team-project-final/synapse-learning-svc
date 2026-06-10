package com.synapse.learning.card.application.port.in;

import com.synapse.learning.card.adapter.in.web.dto.CardCreateRequest;
import com.synapse.learning.card.adapter.in.web.dto.CardResponse;
import com.synapse.learning.card.adapter.in.web.dto.CardUpdateRequest;
import com.synapse.learning.global.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CardUseCase {

    CardResponse createCard(String userId, String tenantId, String deckId, CardCreateRequest request);

    List<CardResponse> createCards(String userId, String tenantId, String deckId, List<CardCreateRequest> requests);

    PageResponse<CardResponse> getCards(String userId, String tenantId, String deckId, Pageable pageable);

    CardResponse getCard(String userId, String tenantId, String deckId, String cardId);

    CardResponse updateCard(String userId, String tenantId, String deckId, String cardId, CardUpdateRequest request);

    void deleteCard(String userId, String tenantId, String deckId, String cardId);
}
