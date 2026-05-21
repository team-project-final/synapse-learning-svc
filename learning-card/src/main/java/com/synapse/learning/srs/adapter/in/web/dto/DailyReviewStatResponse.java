package com.synapse.learning.srs.adapter.in.web.dto;

import java.time.LocalDate;

public record DailyReviewStatResponse(
        LocalDate date,
        long reviewCount,
        double correctRate) {
    public static DailyReviewStatResponse empty(LocalDate date) {
        return new DailyReviewStatResponse(date, 0, 0.0);
    }

}
