package com.synapse.learning.card.adapter.in.web;

import com.synapse.learning.card.adapter.in.web.dto.DeckCopyRequest;
import com.synapse.learning.card.adapter.in.web.dto.DeckCopyResponse;
import com.synapse.learning.card.adapter.in.web.dto.DeckCreateRequest;
import com.synapse.learning.card.adapter.in.web.dto.DeckResponse;
import com.synapse.learning.card.adapter.in.web.dto.DeckShareableResponse;
import com.synapse.learning.card.adapter.in.web.dto.DeckUpdateRequest;
import com.synapse.learning.card.adapter.in.web.dto.SharedDeckDetailResponse;
import com.synapse.learning.card.application.port.in.DeckUseCase;
import com.synapse.learning.global.ApiResponse;
import com.synapse.learning.global.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/decks")
@RequiredArgsConstructor
public class DeckController {

    private final DeckUseCase deckUseCase;

    @PostMapping
    public ResponseEntity<ApiResponse<DeckResponse>> createDeck(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @Valid @RequestBody DeckCreateRequest request) {
        DeckResponse response = deckUseCase.createDeck(userId, tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<DeckResponse>>> getMyDecks(
            @RequestHeader("X-User-Id") String userId,
            @PageableDefault(size = 20) Pageable pageable) {
        Pageable capped = PageRequest.of(
                pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), 100),
                pageable.getSort());
        return ResponseEntity.ok(ApiResponse.success(deckUseCase.getMyDecks(userId, capped)));
    }

    @GetMapping("/{deckId}")
    public ResponseEntity<ApiResponse<DeckResponse>> getDeck(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String deckId) {
        return ResponseEntity.ok(ApiResponse.success(deckUseCase.getDeck(userId, deckId)));
    }

    @PatchMapping("/{deckId}")
    public ResponseEntity<ApiResponse<DeckResponse>> updateDeck(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String deckId,
            @Valid @RequestBody DeckUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(deckUseCase.updateDeck(userId, deckId, request)));
    }

    @DeleteMapping("/{deckId}")
    public ResponseEntity<ApiResponse<Void>> deleteDeck(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String deckId) {
        deckUseCase.deleteDeck(userId, deckId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{deckId}/shared-detail")
    public ResponseEntity<ApiResponse<SharedDeckDetailResponse>> getSharedDeckDetail(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String deckId,
            @RequestParam(required = false) String sharedContentId,
            @RequestParam(required = false) String shareToken) {
        return ResponseEntity.ok(ApiResponse.success(
                deckUseCase.getSharedDeckDetail(userId, deckId, sharedContentId, shareToken)));
    }

    @PostMapping("/{deckId}/copy-from-share")
    public ResponseEntity<ApiResponse<DeckCopyResponse>> copyFromShare(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String deckId,
            @RequestBody DeckCopyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                deckUseCase.copyFromShare(userId, tenantId, deckId, request)));
    }

    @GetMapping("/{deckId}/shareable")
    public ResponseEntity<ApiResponse<DeckShareableResponse>> checkShareable(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String deckId) {
        return ResponseEntity.ok(ApiResponse.success(
                deckUseCase.checkShareable(userId, tenantId, deckId)));
    }
}
