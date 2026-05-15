package com.synapse.learning.card.application;

import com.synapse.learning.card.api.DeckCreateRequest;
import com.synapse.learning.card.api.DeckResponse;
import com.synapse.learning.card.api.DeckUpdateRequest;
import com.synapse.learning.card.domain.exception.DeckNotFoundException;
import com.synapse.learning.card.domain.model.CardDeck;
import com.synapse.learning.card.domain.repository.CardDeckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeckService {

    private final CardDeckRepository cardDeckRepository;
    private final CardDeckMapper cardDeckMapper;

    // 덱 생성
    @Transactional
    public DeckResponse createDeck(String userId, String tenantId, DeckCreateRequest request) {
        CardDeck deck = CardDeck.builder()
                .userId(UUID.fromString(userId))
                .tenantId(tenantId)
                .name(request.name())
                .description(request.description())
                .color(request.color())
                .build();
        return cardDeckMapper.toResponse(cardDeckRepository.save(deck));
    }

    // 내 덱 목록 조회
    public List<DeckResponse> getMyDecks(String userId) {
        return cardDeckRepository
                .findAllByUserIdAndDeletedAtIsNull(UUID.fromString(userId))
                .stream()
                .map(cardDeckMapper::toResponse)
                .toList();
    }

    // 덱 상세 조회
    public DeckResponse getDeck(String deckId) {
        CardDeck deck = findActiveDeck(deckId);
        return cardDeckMapper.toResponse(deck);
    }

    // 덱 수정
    @Transactional
    public DeckResponse updateDeck(String userId, String deckId, DeckUpdateRequest request) {
        CardDeck deck = findActiveDeck(deckId);
        validateOwner(deck, userId);
        deck.update(request.name(), request.description(), request.color());
        return cardDeckMapper.toResponse(deck);
    }

    // 덱 삭제 (Soft Delete)
    @Transactional
    public void deleteDeck(String userId, String deckId) {
        CardDeck deck = findActiveDeck(deckId);
        validateOwner(deck, userId);
        deck.softDelete();
    }

    // ── 내부 헬퍼 ──────────────────────────────────
    private CardDeck findActiveDeck(String deckId) {
        return cardDeckRepository
                .findByIdAndDeletedAtIsNull(UUID.fromString(deckId))
                .orElseThrow(() -> new DeckNotFoundException(deckId));
    }

    private void validateOwner(CardDeck deck, String userId) {
        if (!deck.getUserId().equals(UUID.fromString(userId))) {
            throw new com.synapse.learning.shared.exception.BusinessException(
                    com.synapse.learning.shared.exception.ErrorCode.DECK_ACCESS_DENIED);
        }
    }
}