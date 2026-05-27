package com.synapse.learning.srs.application.port.out;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ReviewStatsPort {

    List<DailyStat> findDailyStats(UUID userId, UUID tenantId, Instant from, Instant to);

    List<WeeklyStat> findWeeklyStats(UUID userId, UUID tenantId, Instant from, Instant to);

    record DailyStat(LocalDate date, long reviewCount, long correctCount) {
    }

    record WeeklyStat(LocalDate weekStart, long reviewCount, long correctCount) {
    }
}