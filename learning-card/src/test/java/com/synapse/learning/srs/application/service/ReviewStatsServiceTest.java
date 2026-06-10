package com.synapse.learning.srs.application.service;

import com.synapse.learning.srs.adapter.in.web.dto.ReviewStatsResponse;
import com.synapse.learning.srs.adapter.in.web.dto.RetentionStatsResponse;
import com.synapse.learning.srs.adapter.in.web.dto.WeeklyStatsResponse;
import com.synapse.learning.srs.application.port.out.ReviewStatsPort;
import com.synapse.learning.srs.application.port.out.StreakPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ReviewStatsServiceTest {

    @Mock
    ReviewStatsPort reviewStatsPort;

    @Mock
    StreakPort streakPort;

    @InjectMocks
    ReviewStatsService reviewStatsService;

    private static final String TENANT_ID = "00000000-0000-0000-0000-000000000099";
    private static final String USER_ID = "00000000-0000-0000-0000-000000000001";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Test
    @DisplayName("getOverview — DB에 데이터가 있는 날은 reviewCount와 correctRate가 채워진다")
    void getOverview_withData_fillsCorrectly() {
        LocalDate today = LocalDate.now(KST);
        ReviewStatsPort.DailyStat stat = new ReviewStatsPort.DailyStat(today, 10L, 8L);

        given(reviewStatsPort.findDailyStats(
                eq(UUID.fromString(USER_ID)), eq(UUID.fromString(TENANT_ID)),
                any(Instant.class), any(Instant.class)))
                .willReturn(List.of(stat));
        given(streakPort.getStreak(USER_ID, TENANT_ID))
                .willReturn(new StreakPort.StreakData(0, 0));

        ReviewStatsResponse response = reviewStatsService.getOverview(TENANT_ID, USER_ID);

        assertThat(response.daily()).hasSize(30);
        assertThat(response.totalReviews()).isEqualTo(10);
        assertThat(response.overallCorrectRate()).isEqualTo(80.0);

        var todayStat = response.daily().stream()
                .filter(d -> d.date().equals(today))
                .findFirst().orElseThrow();
        assertThat(todayStat.reviewCount()).isEqualTo(10);
        assertThat(todayStat.correctRate()).isEqualTo(80.0);
    }

    @Test
    @DisplayName("getOverview — 데이터 없는 날짜는 reviewCount=0, correctRate=0.0으로 채워진다")
    void getOverview_emptyDays_filledWithZero() {
        given(reviewStatsPort.findDailyStats(any(), any(), any(), any()))
                .willReturn(List.of());
        given(streakPort.getStreak(USER_ID, TENANT_ID))
                .willReturn(new StreakPort.StreakData(0, 0));

        ReviewStatsResponse response = reviewStatsService.getOverview(TENANT_ID, USER_ID);

        assertThat(response.daily()).hasSize(30);
        assertThat(response.daily()).allMatch(d -> d.reviewCount() == 0 && d.correctRate() == 0.0);
        assertThat(response.totalReviews()).isEqualTo(0);
        assertThat(response.overallCorrectRate()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("getOverview — correctRate는 소수점 1자리로 반올림된다")
    void getOverview_correctRate_roundsToOneDecimal() {
        LocalDate today = LocalDate.now(KST);
        ReviewStatsPort.DailyStat stat = new ReviewStatsPort.DailyStat(today, 3L, 2L); // 66.666...%

        given(reviewStatsPort.findDailyStats(any(), any(), any(), any()))
                .willReturn(List.of(stat));
        given(streakPort.getStreak(USER_ID, TENANT_ID))
                .willReturn(new StreakPort.StreakData(0, 0));

        ReviewStatsResponse response = reviewStatsService.getOverview(TENANT_ID, USER_ID);

        assertThat(response.overallCorrectRate()).isEqualTo(66.7);
    }

    @Test
    @DisplayName("getHeatmap — 12주치 데이터가 반환되고 빈 주는 0으로 채워진다")
    void getHeatmap_returns12Weeks_emptyFilledWithZero() {
        given(reviewStatsPort.findWeeklyStats(any(), any(), any(), any()))
                .willReturn(List.of());

        WeeklyStatsResponse response = reviewStatsService.getHeatmap(TENANT_ID, USER_ID);

        assertThat(response.weekly()).hasSize(12);
        assertThat(response.weekly()).allMatch(w -> w.reviewCount() == 0 && w.correctRate() == 0.0);
    }

    @Test
    @DisplayName("getHeatmap — 데이터가 있는 주는 correctRate가 올바르게 계산된다")
    void getHeatmap_withData_calculatesCorrectRate() {
        LocalDate currentWeekStart = LocalDate.now(KST).with(DayOfWeek.MONDAY);
        ReviewStatsPort.WeeklyStat stat = new ReviewStatsPort.WeeklyStat(currentWeekStart, 20L, 15L);

        given(reviewStatsPort.findWeeklyStats(any(), any(), any(), any()))
                .willReturn(List.of(stat));

        WeeklyStatsResponse response = reviewStatsService.getHeatmap(TENANT_ID, USER_ID);

        assertThat(response.weekly()).hasSize(12);
        var thisWeek = response.weekly().getLast();
        assertThat(thisWeek.reviewCount()).isEqualTo(20);
        assertThat(thisWeek.correctRate()).isEqualTo(75.0);
    }

    @Test
    @DisplayName("getRetention returns 30 day curve with empty days filled")
    void getRetention_returns30DayCurve_emptyFilledWithZero() {
        given(reviewStatsPort.findDailyStats(any(), any(), any(), any()))
                .willReturn(List.of());

        RetentionStatsResponse response = reviewStatsService.getRetention(TENANT_ID, USER_ID);

        assertThat(response.points()).hasSize(30);
        assertThat(response.points().getFirst().daysAgo()).isEqualTo(29);
        assertThat(response.points().getLast().daysAgo()).isEqualTo(0);
        assertThat(response.points()).allMatch(p -> p.reviewCount() == 0 && p.retentionRate() == 0.0);
    }

    @Test
    @DisplayName("getRetention calculates retention rate from daily correct reviews")
    void getRetention_withData_calculatesRetentionRate() {
        LocalDate today = LocalDate.now(KST);
        ReviewStatsPort.DailyStat stat = new ReviewStatsPort.DailyStat(today, 4L, 3L);

        given(reviewStatsPort.findDailyStats(any(), any(), any(), any()))
                .willReturn(List.of(stat));

        RetentionStatsResponse response = reviewStatsService.getRetention(TENANT_ID, USER_ID);

        var todayPoint = response.points().getLast();
        assertThat(todayPoint.date()).isEqualTo(today);
        assertThat(todayPoint.daysAgo()).isEqualTo(0);
        assertThat(todayPoint.reviewCount()).isEqualTo(4);
        assertThat(todayPoint.retentionRate()).isEqualTo(75.0);
    }
}
