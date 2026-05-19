package com.synapse.learning.srs.domain.model;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "review_sessions")
@Getter
@NoArgsConstructor
public class ReviewSession {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "deck_id", nullable = false)
    private UUID deckId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "total_cards", nullable = false)
    private int totalCards;

    @Column(name = "reviewed_cards", nullable = false)
    private int reviewedCards;

    @Builder
    public ReviewSession(UUID tenantId, UUID userId, UUID deckId, int totalCards) {
        this.tenantId = tenantId;
        this.userId = userId;
        this.deckId = deckId;
        this.status = "in_progress";
        this.startedAt = Instant.now();
        this.totalCards = totalCards;
        this.reviewedCards = 0;
    }

    public void incrementReviewed() {
        this.reviewedCards += 1;
    }

    public void complete() {
        this.status = "completed";
        this.completedAt = Instant.now();
    }

    public void abandon() {
        this.status = "abandoned";
        this.completedAt = Instant.now();
    }
}