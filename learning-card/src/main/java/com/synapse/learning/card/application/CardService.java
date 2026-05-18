package com.synapse.learning.card.application;

import com.synapse.learning.card.api.CardCreateRequest;
import com.synapse.learning.card.api.CardResponse;
import com.synapse.learning.card.api.CardUpdateRequest;
import com.synapse.learning.card.domain.exception.CardNotFoundException;
import com.synapse.learning.card.domain.model.FlashCard;
import com.synapse.learning.card.domain.repository.FlashCardRepository;
import com.synapse.learning.card.domain.repository.CardDeckRepository;
import com.synapse.learning.card.domain.exception.DeckNotFoundException;
import com.synapse.learning.card.domain.model.CardDeck;
import com.synapse.learning.shared.exception.BusinessException;
import com.synapse.learning.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardService {

    private final FlashCardRepository flashCardRepository;
    private final CardDeckRepository cardDeckRepository;
    private final CardMapper cardMapper;

    // 카드 생성
    @Transactional
    public CardResponse createCard(String userId, String tenantId, String deckId, CardCreateRequest request) {
        CardDeck deck = findActiveDeck(deckId);
        validateDeckOwner(deck, userId);

        FlashCard card = FlashCard.builder()
                .deckId(UUID.fromString(deckId))
                .tenantId(UUID.fromString(tenantId))
                .cardType(request.cardType())
                .frontContent(request.frontContent())
                .backContent(request.backContent())
                .sourceId(request.sourceId())
                .bloomLevel(request.bloomLevel())
                .build();

        return cardMapper.toResponse(flashCardRepository.save(card));
    }

    // 카드 목록 조회
    public List<CardResponse> getCards(String userId, String deckId) {
        CardDeck deck = findActiveDeck(deckId);
        validateDeckOwner(deck, userId);

        return flashCardRepository
                .findAllByDeckIdAndDeletedAtIsNull(UUID.fromString(deckId))
                .stream()
                .map(cardMapper::toResponse)
                .toList();
    }

    // 카드 상세 조회
    public CardResponse getCard(String userId, String deckId, String cardId) {
        CardDeck deck = findActiveDeck(deckId);
        validateDeckOwner(deck, userId);
        FlashCard card = findActiveCard(cardId);
        return cardMapper.toResponse(card);
    }

    // 카드 수정
    @Transactional
    public CardResponse updateCard(String userId, String deckId, String cardId, CardUpdateRequest request) {
        CardDeck deck = findActiveDeck(deckId);
        validateDeckOwner(deck, userId);
        FlashCard card = findActiveCard(cardId);
        card.update(request.frontContent(), request.backContent(), request.cardType());
        FlashCard saved = flashCardRepository.saveAndFlush(card);
        return cardMapper.toResponse(saved);
    }

    // 카드 삭제 (Soft Delete)
    @Transactional
    public void deleteCard(String userId, String deckId, String cardId) {
        CardDeck deck = findActiveDeck(deckId);
        validateDeckOwner(deck, userId);
        FlashCard card = findActiveCard(cardId);
        card.softDelete();
    }

    // ── 내부 헬퍼 ──────────────────────────────────
    private CardDeck findActiveDeck(String deckId) {
        return cardDeckRepository
                .findByIdAndDeletedAtIsNull(UUID.fromString(deckId))
                .orElseThrow(() -> new DeckNotFoundException(deckId));
    }

    private void validateDeckOwner(CardDeck deck, String userId) {
        if (!deck.getUserId().equals(UUID.fromString(userId))) {
            throw new BusinessException(ErrorCode.DECK_ACCESS_DENIED);
        }
    }

    private FlashCard findActiveCard(String cardId) {
        return flashCardRepository
                .findByIdAndDeletedAtIsNull(UUID.fromString(cardId))
                .orElseThrow(() -> new CardNotFoundException(cardId));
    }
}