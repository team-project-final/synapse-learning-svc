package com.synapse.learning.card.api;

import com.synapse.learning.shared.ApiResponse;
import com.synapse.learning.card.application.DeckService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/decks")
@RequiredArgsConstructor
public class DeckController {

    private final DeckService deckService;

    // 덱 생성
    @PostMapping
    public ResponseEntity<ApiResponse<DeckResponse>> createDeck(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @Valid @RequestBody DeckCreateRequest request) {
        DeckResponse response = deckService.createDeck(userId, tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    // 내 덱 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<DeckResponse>>> getMyDecks(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(ApiResponse.success(deckService.getMyDecks(userId)));
    }

    // 덱 상세 조회
    @GetMapping("/{deckId}")
    public ResponseEntity<ApiResponse<DeckResponse>> getDeck(
            @PathVariable String deckId) {
        return ResponseEntity.ok(ApiResponse.success(deckService.getDeck(deckId)));
    }

    // 덱 수정
    @PatchMapping("/{deckId}")
    public ResponseEntity<ApiResponse<DeckResponse>> updateDeck(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String deckId,
            @Valid @RequestBody DeckUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(deckService.updateDeck(userId, deckId, request)));
    }

    // 덱 삭제
    @DeleteMapping("/{deckId}")
    public ResponseEntity<ApiResponse<Void>> deleteDeck(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String deckId) {
        deckService.deleteDeck(userId, deckId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}