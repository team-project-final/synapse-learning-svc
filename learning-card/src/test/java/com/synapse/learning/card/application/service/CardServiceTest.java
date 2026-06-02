package com.synapse.learning.card.application.service;

import com.synapse.learning.card.adapter.in.web.dto.CardCreateRequest;
import com.synapse.learning.card.adapter.in.web.dto.CardResponse;
import com.synapse.learning.card.adapter.in.web.dto.CardUpdateRequest;
import com.synapse.learning.card.application.port.out.CardDeckPort;
import com.synapse.learning.card.application.port.out.FlashCardPort;
import com.synapse.learning.card.domain.exception.CardNotFoundException;
import com.synapse.learning.card.domain.exception.DeckNotFoundException;
import com.synapse.learning.card.domain.model.CardDeck;
import com.synapse.learning.card.domain.model.FlashCard;
import com.synapse.learning.global.PageResponse;
import com.synapse.learning.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
class CardServiceTest {

    @Mock
    FlashCardPort flashCardPort;
    @Mock
    CardDeckPort cardDeckPort;
    @Mock
    CardMapper cardMapper;

    @InjectMocks
    CardService cardService;

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    private static final UUID USER_ID   = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID DECK_ID   = UUID.randomUUID();
    private static final UUID CARD_ID   = UUID.randomUUID();

    private CardDeck mockDeck() {
        return CardDeck.builder()
                .userId(USER_ID)
                .tenantId(TENANT_ID)
                .name("테스트 덱")
                .build();
    }

    private FlashCard mockCard() {
        return FlashCard.builder()
                .deckId(DECK_ID)
                .tenantId(TENANT_ID)
                .cardType("qa")
                .frontContent("스택이란?")
                .backContent("LIFO")
                .build();
    }

    private CardResponse mockResponse() {
        return new CardResponse(CARD_ID, DECK_ID, "qa", "스택이란?", "LIFO",
                null, "new", 2.5, 0, 0, 0, Instant.now(), null, Instant.now(), Instant.now());
    }

    // ── createCard ───────────────────────────────────

    @Test
    @DisplayName("카드 생성 성공")
    void createCard_success() {
        FlashCard card = mockCard();
        CardResponse response = mockResponse();

        given(cardDeckPort.findByIdAndDeletedAtIsNull(DECK_ID)).willReturn(Optional.of(mockDeck()));
        given(flashCardPort.save(any())).willReturn(card);
        given(cardMapper.toResponse(card)).willReturn(response);

        CardResponse result = cardService.createCard(
                USER_ID.toString(), TENANT_ID.toString(), DECK_ID.toString(),
                new CardCreateRequest("스택이란?", "LIFO", "qa", null, null));

        assertThat(result.frontContent()).isEqualTo("스택이란?");
        verify(flashCardPort).save(any(FlashCard.class));
    }

    @Test
    @DisplayName("카드 생성 실패 — 덱 없음")
    void createCard_deckNotFound_throwsException() {
        given(cardDeckPort.findByIdAndDeletedAtIsNull(DECK_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.createCard(
                USER_ID.toString(), TENANT_ID.toString(), DECK_ID.toString(),
                new CardCreateRequest("Q", "A", "qa", null, null)))
                .isInstanceOf(DeckNotFoundException.class);
    }

    @Test
    @DisplayName("카드 생성 실패 — 덱 소유자 아님")
    void createCard_notOwner_throwsException() {
        UUID otherUserId = UUID.randomUUID();
        given(cardDeckPort.findByIdAndDeletedAtIsNull(DECK_ID)).willReturn(Optional.of(mockDeck()));

        assertThatThrownBy(() -> cardService.createCard(
                otherUserId.toString(), TENANT_ID.toString(), DECK_ID.toString(),
                new CardCreateRequest("Q", "A", "qa", null, null)))
                .isInstanceOf(BusinessException.class);
    }

    // ── getCards ─────────────────────────────────────

    @Test
    @DisplayName("카드 목록 조회 성공")
    void getCards_success() {
        FlashCard card = mockCard();
        CardResponse response = mockResponse();
        Pageable pageable = PageRequest.of(0, 20);
        Page<FlashCard> pageResult = new PageImpl<>(List.of(card), pageable, 1);

        given(cardDeckPort.findByIdAndDeletedAtIsNull(DECK_ID)).willReturn(Optional.of(mockDeck()));
        given(flashCardPort.findAllByDeckIdAndDeletedAtIsNull(DECK_ID, pageable)).willReturn(pageResult);
        given(cardMapper.toResponse(card)).willReturn(response);

        PageResponse<CardResponse> result = cardService.getCards(USER_ID.toString(), TENANT_ID.toString(), DECK_ID.toString(), pageable);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).frontContent()).isEqualTo("스택이란?");
    }

    // ── updateCard ───────────────────────────────────

    @Test
    @DisplayName("카드 수정 성공")
    void updateCard_success() {
        FlashCard card = mockCard();
        CardResponse response = mockResponse();

        given(cardDeckPort.findByIdAndDeletedAtIsNull(DECK_ID)).willReturn(Optional.of(mockDeck()));
        given(flashCardPort.findByIdAndDeletedAtIsNull(CARD_ID)).willReturn(Optional.of(card));
        given(flashCardPort.saveAndFlush(any())).willReturn(card);
        given(cardMapper.toResponse(card)).willReturn(response);

        CardResponse result = cardService.updateCard(
                USER_ID.toString(), TENANT_ID.toString(), DECK_ID.toString(), CARD_ID.toString(),
                new CardUpdateRequest("수정된 질문", "수정된 답", "qa"));

        assertThat(result).isNotNull();
        verify(flashCardPort).saveAndFlush(any());
    }

    // ── deleteCard ───────────────────────────────────

    @Test
    @DisplayName("카드 삭제(softDelete) 성공")
    void deleteCard_success() {
        FlashCard card = mockCard();

        given(cardDeckPort.findByIdAndDeletedAtIsNull(DECK_ID)).willReturn(Optional.of(mockDeck()));
        given(flashCardPort.findByIdAndDeletedAtIsNull(CARD_ID)).willReturn(Optional.of(card));

        cardService.deleteCard(USER_ID.toString(), TENANT_ID.toString(), DECK_ID.toString(), CARD_ID.toString());

        assertThat(card.getDeletedAt()).isNotNull();
    }

    // ── getCard ──────────────────────────────────────

    @Test
    @DisplayName("카드 조회 실패 — 카드 없음")
    void getCard_notFound_throwsException() {
        given(cardDeckPort.findByIdAndDeletedAtIsNull(DECK_ID)).willReturn(Optional.of(mockDeck()));
        given(flashCardPort.findByIdAndDeletedAtIsNull(CARD_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.getCard(
                USER_ID.toString(), TENANT_ID.toString(), DECK_ID.toString(), CARD_ID.toString()))
                .isInstanceOf(CardNotFoundException.class);
    }
}
