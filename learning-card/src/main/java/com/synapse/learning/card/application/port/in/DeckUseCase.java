package com.synapse.learning.card.application.port.in;

import com.synapse.learning.card.adapter.in.web.dto.DeckCreateRequest;
import com.synapse.learning.card.adapter.in.web.dto.DeckResponse;
import com.synapse.learning.card.adapter.in.web.dto.DeckUpdateRequest;
import com.synapse.learning.global.PageResponse;
import org.springframework.data.domain.Pageable;

public interface DeckUseCase {

    DeckResponse createDeck(String userId, String tenantId, DeckCreateRequest request);

    PageResponse<DeckResponse> getMyDecks(String userId, Pageable pageable);

    DeckResponse getDeck(String userId, String deckId);

    DeckResponse updateDeck(String userId, String deckId, DeckUpdateRequest request);

    void deleteDeck(String userId, String deckId);
}
