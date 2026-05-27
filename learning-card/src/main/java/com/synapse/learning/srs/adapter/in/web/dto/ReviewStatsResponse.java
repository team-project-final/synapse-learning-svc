package com.synapse.learning.srs.adapter.in.web.dto;

import java.util.List;

public record ReviewStatsResponse(
        List<DailyReviewStatResponse> daily,
        long totalReviews,
        double overallCorrectRate) {

}
