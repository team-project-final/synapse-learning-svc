package com.synapse.learning.srs.api;

import com.synapse.learning.shared.ApiResponse;
import com.synapse.learning.srs.application.ReviewSessionService;
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

    private final ReviewSessionService reviewSessionService;

    // 세션 시작
    @PostMapping("/sessions")
    public ResponseEntity<ApiResponse<ReviewSessionResponse>> startSession(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestBody @Valid ReviewSessionStartRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                reviewSessionService.startSession(tenantId, userId, request)));
    }

    // 오늘 복습 카드 큐
    @GetMapping("/queue")
    public ResponseEntity<ApiResponse<List<ReviewCardResponse>>> getReviewQueue(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam UUID deckId) {
        return ResponseEntity.ok(ApiResponse.success(
                reviewSessionService.getReviewQueue(tenantId, deckId)));
    }

    // 카드 rating 제출
    @PostMapping("/sessions/{sessionId}/submit")
    public ResponseEntity<ApiResponse<ReviewSubmitResponse>> submitReview(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID sessionId,
            @RequestBody @Valid ReviewSessionSubmitRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                reviewSessionService.submitReview(tenantId, userId, sessionId, request)));
    }

    // 세션 완료
    @PutMapping("/sessions/{sessionId}/complete")
    public ResponseEntity<ApiResponse<ReviewSessionResponse>> completeSession(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID sessionId) {
        return ResponseEntity.ok(ApiResponse.success(
                reviewSessionService.completeSession(tenantId, sessionId)));
    }
}