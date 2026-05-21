package com.synapse.learning.srs.adapter.in.web.dto;

import java.time.LocalDate;

public record WeeklyReviewStatResponse(
        LocalDate weekStart,
        long reviewCount,
        double correctRate) {
    public static WeeklyReviewStatResponse empty(LocalDate weekStart) {
        return new WeeklyReviewStatResponse(weekStart, 0, 0.0);
    }

}
