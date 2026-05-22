package com.synapse.learning.srs.adapter.out.persistence;

import com.synapse.learning.srs.application.port.out.ReviewStatsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReviewStatsPersistenceAdapter implements ReviewStatsPort {

    private final ReviewStatsJpaRepository jpaRepository;

    @Override
    public List<DailyStat> findDailyStats(UUID userId, UUID tenantId, Instant from, Instant to) {
        return jpaRepository.findDailyStats(userId, tenantId, from, to)
                .stream()
                .map(r -> new DailyStat(r.getReviewDate(), r.getReviewCount(), r.getCorrectCount()))
                .toList();
    }

    @Override
    public List<WeeklyStat> findWeeklyStats(UUID userId, UUID tenantId, Instant from, Instant to) {
        return jpaRepository.findWeeklyStats(userId, tenantId, from, to)
                .stream()
                .map(r -> new WeeklyStat(r.getWeekStart(), r.getReviewCount(), r.getCorrectCount()))
                .toList();
    }
}