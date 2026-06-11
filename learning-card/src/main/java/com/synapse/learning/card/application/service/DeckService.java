package com.synapse.learning.card.application.service;

import com.synapse.learning.card.adapter.in.web.dto.CardResponse;
import com.synapse.learning.card.adapter.in.web.dto.DeckCopyRequest;
import com.synapse.learning.card.adapter.in.web.dto.DeckCopyResponse;
import com.synapse.learning.card.adapter.in.web.dto.DeckCreateRequest;
import com.synapse.learning.card.adapter.in.web.dto.DeckResponse;
import com.synapse.learning.card.adapter.in.web.dto.DeckShareableResponse;
import com.synapse.learning.card.adapter.in.web.dto.DeckUpdateRequest;
import com.synapse.learning.card.adapter.in.web.dto.SharedDeckDetailResponse;
import com.synapse.learning.card.application.port.in.DeckUseCase;
import com.synapse.learning.card.application.port.out.CardDeckPort;
import com.synapse.learning.card.application.port.out.FlashCardPort;
import com.synapse.learning.card.domain.exception.DeckNotFoundException;
import com.synapse.learning.card.domain.model.CardDeck;
import com.synapse.learning.card.domain.model.FlashCard;
import com.synapse.learning.global.PageResponse;
import com.synapse.learning.global.exception.BusinessException;
import com.synapse.learning.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeckService implements DeckUseCase {

    private final CardDeckPort cardDeckPort;
    private final FlashCardPort flashCardPort;
    private final CardDeckMapper cardDeckMapper;
    private final CardMapper cardMapper;

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
    public DeckResponse getDeck(String userId, String deckId) {
        CardDeck deck = findActiveDeck(deckId);
        validateOwner(deck, userId);
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

    @Override
    public SharedDeckDetailResponse getSharedDeckDetail(String userId, String deckId, String sharedContentId, String shareToken) {
        CardDeck deck = findActiveDeck(deckId);
        List<CardResponse> cards = flashCardPort.findAllByDeckIdAndDeletedAtIsNull(UUID.fromString(deckId))
                .stream()
                .map(cardMapper::toResponse)
                .toList();
        return new SharedDeckDetailResponse(cardDeckMapper.toResponse(deck), cards);
    }

    @Override
    @Transactional
    public DeckCopyResponse copyFromShare(String userId, String tenantId, String deckId, DeckCopyRequest request) {
        CardDeck origin = findActiveDeck(deckId);
        List<FlashCard> originCards = flashCardPort.findAllByDeckIdAndDeletedAtIsNull(UUID.fromString(deckId));

        CardDeck newDeck = CardDeck.builder()
                .userId(UUID.fromString(userId))
                .tenantId(UUID.fromString(tenantId))
                .name(origin.getName())
                .description(origin.getDescription())
                .color(origin.getColor())
                .build();
        newDeck = cardDeckPort.save(newDeck);

        UUID newDeckId = newDeck.getId();
        List<FlashCard> copies = originCards.stream()
                .map(c -> FlashCard.builder()
                        .deckId(newDeckId)
                        .tenantId(UUID.fromString(tenantId))
                        .cardType(c.getCardType())
                        .frontContent(c.getFrontContent())
                        .backContent(c.getBackContent())
                        .bloomLevel(c.getBloomLevel())
                        .build())
                .toList();
        flashCardPort.saveAll(copies);

        return new DeckCopyResponse(cardDeckMapper.toResponse(newDeck), copies.size());
    }

    @Override
    public DeckShareableResponse checkShareable(String userId, String tenantId, String deckId) {
        CardDeck deck = findActiveDeck(deckId);
        if (!deck.getUserId().equals(UUID.fromString(userId))) {
            return new DeckShareableResponse(deckId, false, null, null, null, null, "NOT_OWNER");
        }
        if (!deck.getTenantId().equals(UUID.fromString(tenantId))) {
            return new DeckShareableResponse(deckId, false, null, null, null, null, "TENANT_MISMATCH");
        }
        int cardCount = flashCardPort.findAllByDeckIdAndDeletedAtIsNull(UUID.fromString(deckId)).size();
        return new DeckShareableResponse(deckId, true, deck.getName(), deck.getDescription(), deck.getColor(), cardCount, null);
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
