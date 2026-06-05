package com.synapse.learning.card.application.service;

import com.synapse.learning.card.adapter.in.web.dto.CardCreateRequest;
import com.synapse.learning.card.adapter.in.web.dto.CardResponse;
import com.synapse.learning.card.adapter.in.web.dto.CardUpdateRequest;
import com.synapse.learning.card.application.port.in.CardUseCase;
import com.synapse.learning.card.application.port.out.CardDeckPort;
import com.synapse.learning.card.application.port.out.FlashCardPort;
import com.synapse.learning.card.domain.exception.CardNotFoundException;
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
public class CardService implements CardUseCase {

    private final FlashCardPort flashCardPort;
    private final CardDeckPort cardDeckPort;
    private final CardMapper cardMapper;

    @Override
    @Transactional
    public CardResponse createCard(String userId, String tenantId, String deckId, CardCreateRequest request) {
        CardDeck deck = findActiveDeck(deckId);
        validateDeckOwner(deck, userId, tenantId);

        return createCard(deckId, tenantId, request);
    }

    @Override
    @Transactional
    public List<CardResponse> createCards(String userId, String tenantId, String deckId, List<CardCreateRequest> requests) {
        CardDeck deck = findActiveDeck(deckId);
        validateDeckOwner(deck, userId, tenantId);

        return requests.stream()
                .map(request -> createCard(deckId, tenantId, request))
                .toList();
    }

    private CardResponse createCard(String deckId, String tenantId, CardCreateRequest request) {
        FlashCard card = FlashCard.builder()
                .deckId(UUID.fromString(deckId))
                .tenantId(UUID.fromString(tenantId))
                .cardType(request.cardType())
                .frontContent(request.frontContent())
                .backContent(request.backContent())
                .sourceId(request.sourceId() != null ? UUID.fromString(request.sourceId()) : null)
                .bloomLevel(request.bloomLevel())
                .build();

        return cardMapper.toResponse(flashCardPort.save(card));
    }

    @Override
    public PageResponse<CardResponse> getCards(String userId, String tenantId, String deckId, Pageable pageable) {
        CardDeck deck = findActiveDeck(deckId);
        validateDeckOwner(deck, userId, tenantId);
        Page<CardResponse> page = flashCardPort
                .findAllByDeckIdAndDeletedAtIsNull(UUID.fromString(deckId), pageable)
                .map(cardMapper::toResponse);
        return PageResponse.from(page);
    }

    @Override
    public CardResponse getCard(String userId, String tenantId, String deckId, String cardId) {
        CardDeck deck = findActiveDeck(deckId);
        validateDeckOwner(deck, userId, tenantId);
        FlashCard card = findActiveCard(cardId);
        validateCardInDeck(card, deckId);
        return cardMapper.toResponse(card);
    }

    @Override
    @Transactional
    public CardResponse updateCard(String userId, String tenantId, String deckId, String cardId, CardUpdateRequest request) {
        CardDeck deck = findActiveDeck(deckId);
        validateDeckOwner(deck, userId, tenantId);
        FlashCard card = findActiveCard(cardId);
        validateCardInDeck(card, deckId);
        card.update(request.frontContent(), request.backContent(), request.cardType());
        FlashCard saved = flashCardPort.saveAndFlush(card);
        return cardMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteCard(String userId, String tenantId, String deckId, String cardId) {
        CardDeck deck = findActiveDeck(deckId);
        validateDeckOwner(deck, userId, tenantId);
        FlashCard card = findActiveCard(cardId);
        validateCardInDeck(card, deckId);
        card.softDelete();
    }

    // ── private helpers ──
    private CardDeck findActiveDeck(String deckId) {
        return cardDeckPort
                .findByIdAndDeletedAtIsNull(UUID.fromString(deckId))
                .orElseThrow(() -> new DeckNotFoundException(deckId));
    }

    private void validateDeckOwner(CardDeck deck, String userId, String tenantId) {
        if (!deck.getTenantId().equals(UUID.fromString(tenantId))
                || !deck.getUserId().equals(UUID.fromString(userId))) {
            throw new BusinessException(ErrorCode.DECK_ACCESS_DENIED);
        }
    }

    private FlashCard findActiveCard(String cardId) {
        return flashCardPort
                .findByIdAndDeletedAtIsNull(UUID.fromString(cardId))
                .orElseThrow(() -> new CardNotFoundException(cardId));
    }

    private void validateCardInDeck(FlashCard card, String deckId) {
        if (!card.getDeckId().equals(UUID.fromString(deckId))) {
            throw new BusinessException(ErrorCode.CARD_ACCESS_DENIED);
        }
    }
}
