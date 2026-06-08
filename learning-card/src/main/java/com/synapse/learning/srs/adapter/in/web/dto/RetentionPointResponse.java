package com.synapse.learning.srs.adapter.in.web.dto;

import java.time.LocalDate;

public record RetentionPointResponse(
        LocalDate date,
        int daysAgo,
        long reviewCount,
        double retentionRate) {
    public static RetentionPointResponse empty(LocalDate date, int daysAgo) {
        return new RetentionPointResponse(date, daysAgo, 0, 0.0);
    }
}
