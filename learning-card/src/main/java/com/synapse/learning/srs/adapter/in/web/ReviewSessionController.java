package com.synapse.learning.srs.adapter.in.web;

import com.synapse.learning.global.ApiResponse;
import com.synapse.learning.srs.adapter.in.web.dto.ReviewCardResponse;
import com.synapse.learning.srs.adapter.in.web.dto.ReviewSessionResponse;
import com.synapse.learning.srs.adapter.in.web.dto.ReviewSessionStartRequest;
import com.synapse.learning.srs.adapter.in.web.dto.ReviewSessionSubmitRequest;
import com.synapse.learning.srs.adapter.in.web.dto.ReviewSubmitResponse;
import com.synapse.learning.srs.application.port.in.ReviewSessionUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewSessionController {

    private final ReviewSessionUseCase reviewSessionUseCase;

    @PostMapping("/sessions")
    public ResponseEntity<ApiResponse<ReviewSessionResponse>> startSession(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestBody @Valid ReviewSessionStartRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                reviewSessionUseCase.startSession(tenantId, userId, request)));
    }

    @GetMapping("/queue")
    public ResponseEntity<ApiResponse<List<ReviewCardResponse>>> getReviewQueue(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Id") String userId,
            @RequestParam UUID deckId) {
        return ResponseEntity.ok(ApiResponse.success(
                reviewSessionUseCase.getReviewQueue(tenantId, userId, deckId)));
    }

    @PostMapping("/sessions/{sessionId}/submit")
    public ResponseEntity<ApiResponse<ReviewSubmitResponse>> submitReview(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID sessionId,
            @RequestBody @Valid ReviewSessionSubmitRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                reviewSessionUseCase.submitReview(tenantId, userId, sessionId, request)));
    }

    @PutMapping("/sessions/{sessionId}/complete")
    public ResponseEntity<ApiResponse<ReviewSessionResponse>> completeSession(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID sessionId) {
        return ResponseEntity.ok(ApiResponse.success(
                reviewSessionUseCase.completeSession(tenantId, sessionId)));
    }
}
