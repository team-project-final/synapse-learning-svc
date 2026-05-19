package com.synapse.learning.card.application.service;

import com.synapse.learning.card.adapter.in.web.dto.DeckCreateRequest;
import com.synapse.learning.card.adapter.in.web.dto.DeckResponse;
import com.synapse.learning.card.adapter.in.web.dto.DeckUpdateRequest;
import com.synapse.learning.card.application.port.in.DeckUseCase;
import com.synapse.learning.card.application.port.out.CardDeckPort;
import com.synapse.learning.card.domain.exception.DeckNotFoundException;
import com.synapse.learning.card.domain.model.CardDeck;
import com.synapse.learning.global.PageResponse;
import com.synapse.learning.global.exception.BusinessException;
import com.synapse.learning.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeckService implements DeckUseCase {

    private final CardDeckPort cardDeckPort;
    private final CardDeckMapper cardDeckMapper;

    @Override
    @Transactional
    public DeckResponse createDeck(String userId, String tenantId, DeckCreateRequest request) {
        CardDeck deck = CardDeck.builder()
                .userId(UUID.fromString(userId))
                .tenantId(UUID.fromString(tenantId))
                .name(request.name())
                .description(request.description())
                .color(request.color())
                .build();
        return cardDeckMapper.toResponse(cardDeckPort.save(deck));
    }

    @Override
    public PageResponse<DeckResponse> getMyDecks(String userId, Pageable pageable) {
        Page<DeckResponse> page = cardDeckPort
                .findAllByUserIdAndDeletedAtIsNull(UUID.fromString(userId), pageable)
                .map(cardDeckMapper::toResponse);
        return PageResponse.from(page);
    }

    @Override
    public DeckResponse getDeck(String deckId) {
        CardDeck deck = findActiveDeck(deckId);
        return cardDeckMapper.toResponse(deck);
    }

    @Override
    @Transactional
    public DeckResponse updateDeck(String userId, String deckId, DeckUpdateRequest request) {
        CardDeck deck = findActiveDeck(deckId);
        validateOwner(deck, userId);
        deck.update(request.name(), request.description(), request.color());
        CardDeck saved = cardDeckPort.saveAndFlush(deck);
        return cardDeckMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteDeck(String userId, String deckId) {
        CardDeck deck = findActiveDeck(deckId);
        validateOwner(deck, userId);
        deck.softDelete();
    }

    // ── private helpers ──
    private CardDeck findActiveDeck(String deckId) {
        return cardDeckPort
                .findByIdAndDeletedAtIsNull(UUID.fromString(deckId))
                .orElseThrow(() -> new DeckNotFoundException(deckId));
    }

    private void validateOwner(CardDeck deck, String userId) {
        if (!deck.getUserId().equals(UUID.fromString(userId))) {
            throw new BusinessException(ErrorCode.DECK_ACCESS_DENIED);
        }
    }
}
