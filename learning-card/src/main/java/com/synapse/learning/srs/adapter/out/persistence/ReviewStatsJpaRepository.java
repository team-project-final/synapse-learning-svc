package com.synapse.learning.srs.adapter.out.persistence;

import com.synapse.learning.srs.domain.model.CardReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ReviewStatsJpaRepository extends JpaRepository<CardReview, UUID> {

    @Query(value = """
            SELECT
                DATE_TRUNC('day', cr.reviewed_at AT TIME ZONE 'Asia/Seoul')::date AS review_date,
                COUNT(*)                                     AS review_count,
                COUNT(*) FILTER (WHERE cr.rating >= 3)      AS correct_count
            FROM card_reviews cr
            JOIN review_sessions rs ON cr.session_id = rs.id
            WHERE rs.user_id   = :userId
              AND rs.tenant_id = :tenantId
              AND cr.reviewed_at >= :from
              AND cr.reviewed_at <  :to
            GROUP BY 1
            ORDER BY 1
            """, nativeQuery = true)
    List<DailyStatRow> findDailyStats(
            @Param("userId") UUID userId,
            @Param("tenantId") UUID tenantId,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query(value = """
            SELECT
                DATE_TRUNC('week', cr.reviewed_at AT TIME ZONE 'Asia/Seoul')::date AS week_start,
                COUNT(*)                                     AS review_count,
                COUNT(*) FILTER (WHERE cr.rating >= 3)      AS correct_count
            FROM card_reviews cr
            JOIN review_sessions rs ON cr.session_id = rs.id
            WHERE rs.user_id   = :userId
              AND rs.tenant_id = :tenantId
              AND cr.reviewed_at >= :from
              AND cr.reviewed_at <  :to
            GROUP BY 1
            ORDER BY 1
            """, nativeQuery = true)
    List<WeeklyStatRow> findWeeklyStats(
            @Param("userId") UUID userId,
            @Param("tenantId") UUID tenantId,
            @Param("from") Instant from,
            @Param("to") Instant to);
}