package com.synapse.learning.card.application;

import com.synapse.learning.card.api.DeckCreateRequest;
import com.synapse.learning.card.api.DeckResponse;
import com.synapse.learning.card.api.DeckUpdateRequest;
import com.synapse.learning.card.domain.exception.DeckNotFoundException;
import com.synapse.learning.card.domain.model.CardDeck;
import com.synapse.learning.card.domain.repository.CardDeckRepository;
import com.synapse.learning.shared.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.synapse.learning.shared.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeckServiceTest {

    @Mock
    CardDeckRepository cardDeckRepository;
    @Mock
    CardDeckMapper cardDeckMapper;

    @InjectMocks
    DeckService deckService;

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID DECK_ID = UUID.randomUUID();

    private CardDeck mockDeck() {
        return CardDeck.builder()
                .userId(USER_ID)
                .tenantId(TENANT_ID)
                .name("테스트 덱")
                .build();
    }

    private DeckResponse mockResponse() {
        return new DeckResponse(DECK_ID, "테스트 덱", null, null, Instant.now(), Instant.now());
    }

    // ── createDeck ───────────────────────────────────

    @Test
    @DisplayName("덱 생성 성공")
    void createDeck_success() {
        CardDeck deck = mockDeck();
        DeckResponse response = mockResponse();

        given(cardDeckRepository.save(any())).willReturn(deck);
        given(cardDeckMapper.toResponse(deck)).willReturn(response);

        DeckResponse result = deckService.createDeck(
                USER_ID.toString(), TENANT_ID.toString(),
                new DeckCreateRequest("테스트 덱", null, null));

        assertThat(result.name()).isEqualTo("테스트 덱");
        verify(cardDeckRepository).save(any(CardDeck.class));
    }

    // ── getMyDecks ───────────────────────────────────

    @Test
    @DisplayName("내 덱 목록 조회 성공")
    void getMyDecks_success() {
        CardDeck deck = mockDeck();
        DeckResponse response = mockResponse();
        Pageable pageable = PageRequest.of(0, 20);
        Page<CardDeck> pageResult = new PageImpl<>(List.of(deck), pageable, 1);

        given(cardDeckRepository.findAllByUserIdAndDeletedAtIsNull(USER_ID, pageable))
                .willReturn(pageResult);
        given(cardDeckMapper.toResponse(deck)).willReturn(response);

        PageResponse<DeckResponse> result = deckService.getMyDecks(USER_ID.toString(), pageable);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).name()).isEqualTo("테스트 덱");
    }

    // ── updateDeck ───────────────────────────────────

    @Test
    @DisplayName("덱 수정 성공")
    void updateDeck_success() {
        CardDeck deck = mockDeck();
        DeckResponse response = mockResponse();

        given(cardDeckRepository.findByIdAndDeletedAtIsNull(DECK_ID)).willReturn(Optional.of(deck));
        given(cardDeckRepository.saveAndFlush(any())).willReturn(deck);
        given(cardDeckMapper.toResponse(deck)).willReturn(response);

        DeckResponse result = deckService.updateDeck(
                USER_ID.toString(), DECK_ID.toString(),
                new DeckUpdateRequest("수정된 덱", null, null));

        assertThat(result).isNotNull();
        verify(cardDeckRepository).saveAndFlush(any());
    }

    @Test
    @DisplayName("덱 수정 실패 — 소유자 아님")
    void updateDeck_notOwner_throwsException() {
        UUID otherUserId = UUID.randomUUID();
        given(cardDeckRepository.findByIdAndDeletedAtIsNull(DECK_ID)).willReturn(Optional.of(mockDeck()));

        assertThatThrownBy(() -> deckService.updateDeck(
                otherUserId.toString(), DECK_ID.toString(),
                new DeckUpdateRequest("수정", null, null)))
                .isInstanceOf(BusinessException.class);
    }

    // ── deleteDeck ───────────────────────────────────

    @Test
    @DisplayName("덱 삭제(softDelete) 성공")
    void deleteDeck_success() {
        CardDeck deck = mockDeck();

        given(cardDeckRepository.findByIdAndDeletedAtIsNull(DECK_ID)).willReturn(Optional.of(deck));

        deckService.deleteDeck(USER_ID.toString(), DECK_ID.toString());

        assertThat(deck.getDeletedAt()).isNotNull();
    }

    // ── getDeck ──────────────────────────────────────

    @Test
    @DisplayName("덱 조회 실패 — 덱 없음")
    void getDeck_notFound_throwsException() {
        given(cardDeckRepository.findByIdAndDeletedAtIsNull(DECK_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> deckService.getDeck(DECK_ID.toString()))
                .isInstanceOf(DeckNotFoundException.class);
    }
}