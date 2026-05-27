package com.synapse.learning.srs.application.service;

import com.synapse.learning.srs.adapter.in.web.dto.*;
import com.synapse.learning.srs.application.port.in.ReviewStatsUseCase;
import com.synapse.learning.srs.application.port.out.ReviewStatsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewStatsService implements ReviewStatsUseCase {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private final ReviewStatsPort reviewStatsPort;

    @Override
    public ReviewStatsResponse getOverview(String tenantId, String userId) {
        LocalDate today = LocalDate.now(KST);
        Instant from = today.minusDays(29).atStartOfDay(KST).toInstant();
        Instant to = today.plusDays(1).atStartOfDay(KST).toInstant();

        List<ReviewStatsPort.DailyStat> rows = reviewStatsPort.findDailyStats(
                UUID.fromString(userId), UUID.fromString(tenantId), from, to);

        Map<LocalDate, ReviewStatsPort.DailyStat> rowMap = rows.stream()
                .collect(Collectors.toMap(ReviewStatsPort.DailyStat::date, r -> r));

        List<DailyReviewStatResponse> daily = new ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            ReviewStatsPort.DailyStat row = rowMap.get(date);
            if (row != null) {
                daily.add(new DailyReviewStatResponse(date, row.reviewCount(),
                        toRate(row.correctCount(), row.reviewCount())));
            } else {
                daily.add(DailyReviewStatResponse.empty(date));
            }
        }

        long totalReviews = rows.stream().mapToLong(ReviewStatsPort.DailyStat::reviewCount).sum();
        long totalCorrect = rows.stream().mapToLong(ReviewStatsPort.DailyStat::correctCount).sum();

        return new ReviewStatsResponse(daily, totalReviews, toRate(totalCorrect, totalReviews));
    }

    @Override
    public WeeklyStatsResponse getHeatmap(String tenantId, String userId) {
        LocalDate today = LocalDate.now(KST);
        LocalDate currentWeekStart = today.with(DayOfWeek.MONDAY);
        LocalDate fromDate = currentWeekStart.minusWeeks(11);

        Instant from = fromDate.atStartOfDay(KST).toInstant();
        Instant to = currentWeekStart.plusWeeks(1).atStartOfDay(KST).toInstant();

        List<ReviewStatsPort.WeeklyStat> rows = reviewStatsPort.findWeeklyStats(
                UUID.fromString(userId), UUID.fromString(tenantId), from, to);

        Map<LocalDate, ReviewStatsPort.WeeklyStat> rowMap = rows.stream()
                .collect(Collectors.toMap(ReviewStatsPort.WeeklyStat::weekStart, r -> r));

        List<WeeklyReviewStatResponse> weekly = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            LocalDate weekStart = currentWeekStart.minusWeeks(i);
            ReviewStatsPort.WeeklyStat row = rowMap.get(weekStart);
            if (row != null) {
                weekly.add(new WeeklyReviewStatResponse(weekStart, row.reviewCount(),
                        toRate(row.correctCount(), row.reviewCount())));
            } else {
                weekly.add(WeeklyReviewStatResponse.empty(weekStart));
            }
        }

        return new WeeklyStatsResponse(weekly);
    }

    private double toRate(long correct, long total) {
        if (total == 0)
            return 0.0;
        return Math.round(correct * 1000.0 / total) / 10.0;
    }
}