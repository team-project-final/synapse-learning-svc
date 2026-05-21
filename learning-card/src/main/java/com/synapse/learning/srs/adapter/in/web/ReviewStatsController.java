package com.synapse.learning.srs.adapter.in.web;

import com.synapse.learning.global.ApiResponse;
import com.synapse.learning.srs.adapter.in.web.dto.ReviewStatsResponse;
import com.synapse.learning.srs.adapter.in.web.dto.WeeklyStatsResponse;
import com.synapse.learning.srs.application.port.in.ReviewStatsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class ReviewStatsController {

    private final ReviewStatsUseCase reviewStatsUseCase;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<ReviewStatsResponse>> getOverview(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(ApiResponse.success(
                reviewStatsUseCase.getOverview(tenantId, userId)));
    }

    @GetMapping("/heatmap")
    public ResponseEntity<ApiResponse<WeeklyStatsResponse>> getHeatmap(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(ApiResponse.success(
                reviewStatsUseCase.getHeatmap(tenantId, userId)));
    }
}