package com.synapse.learning.card.api;

import com.synapse.learning.card.application.CardService;
import com.synapse.learning.shared.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/decks/{deckId}/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    @PostMapping
    public ResponseEntity<ApiResponse<CardResponse>> createCard(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String deckId,
            @RequestBody @Valid CardCreateRequest request) {
        CardResponse response = cardService.createCard(userId, tenantId, deckId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CardResponse>>> getCards(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String deckId) {
        return ResponseEntity.ok(ApiResponse.success(cardService.getCards(userId, deckId)));
    }

    @GetMapping("/{cardId}")
    public ResponseEntity<ApiResponse<CardResponse>> getCard(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String deckId,
            @PathVariable String cardId) {
        return ResponseEntity.ok(ApiResponse.success(cardService.getCard(userId, deckId, cardId)));
    }

    @PatchMapping("/{cardId}")
    public ResponseEntity<ApiResponse<CardResponse>> updateCard(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String deckId,
            @PathVariable String cardId,
            @RequestBody @Valid CardUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(cardService.updateCard(userId, deckId, cardId, request)));
    }

    @DeleteMapping("/{cardId}")
    public ResponseEntity<Void> deleteCard(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String deckId,
            @PathVariable String cardId) {
        cardService.deleteCard(userId, deckId, cardId);
        return ResponseEntity.noContent().build();
    }
}