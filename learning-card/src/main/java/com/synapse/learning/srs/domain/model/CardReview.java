package com.synapse.learning.srs.domain.model;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "card_reviews")
@Getter
@NoArgsConstructor
public class CardReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "card_id", nullable = false)
    private UUID cardId;

    @Column(name = "rating", nullable = false)
    private int rating;

    @Column(name = "prev_ease_factor", nullable = false)
    private double prevEaseFactor;

    @Column(name = "new_ease_factor", nullable = false)
    private double newEaseFactor;

    @Column(name = "prev_interval", nullable = false)
    private int prevInterval;

    @Column(name = "new_interval", nullable = false)
    private int newInterval;

    @Column(name = "repetitions", nullable = false)
    private int repetitions;

    @Column(name = "reviewed_at", nullable = false)
    private Instant reviewedAt;

    @Builder
    public CardReview(String tenantId, UUID cardId, int rating,
            double prevEaseFactor, double newEaseFactor,
            int prevInterval, int newInterval, int repetitions) {
        this.tenantId = tenantId;
        this.cardId = cardId;
        this.rating = rating;
        this.prevEaseFactor = prevEaseFactor;
        this.newEaseFactor = newEaseFactor;
        this.prevInterval = prevInterval;
        this.newInterval = newInterval;
        this.repetitions = repetitions;
        this.reviewedAt = Instant.now();
    }
}