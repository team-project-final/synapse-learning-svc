package com.synapse.learning.srs.api;

import com.synapse.learning.shared.ApiResponse;
import com.synapse.learning.srs.application.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cards/{cardId}/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewSubmitResponse>> submitReview(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String cardId,
            @RequestBody @Valid ReviewSubmitRequest request) {
        ReviewSubmitResponse response = reviewService.submitReview(userId, tenantId, cardId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}