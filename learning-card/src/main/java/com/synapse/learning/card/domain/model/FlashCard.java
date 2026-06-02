package com.synapse.learning.card.domain.model;

import com.synapse.learning.global.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cards")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FlashCard extends BaseEntity {

    @Column(name = "deck_id", nullable = false, updatable = false)
    private UUID deckId;

    @Column(name = "source_type", nullable = false, length = 20)
    private String sourceType;

    @Column(name = "source_id")
    private UUID sourceId;

    @Column(name = "card_type", nullable = false, length = 20)
    private String cardType;

    @Column(name = "front_content", nullable = false, columnDefinition = "TEXT")
    private String frontContent;

    @Column(name = "back_content", nullable = false, columnDefinition = "TEXT")
    private String backContent;

    @Column(name = "bloom_level", length = 20)
    private String bloomLevel;

    @Column(name = "easiness_factor", nullable = false)
    private double easinessFactor;

    @Column(name = "interval_days", nullable = false)
    private int intervalDays;

    @Column(name = "repetitions", nullable = false)
    private int repetitions;

    @Column(name = "lapses", nullable = false)
    private int lapses;

    @Column(name = "due_date", nullable = false)
    private Instant dueDate;

    @Column(name = "last_reviewed_at")
    private Instant lastReviewedAt;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Builder
    public FlashCard(UUID deckId, UUID tenantId, String sourceType, UUID sourceId,
            String cardType, String frontContent, String backContent, String bloomLevel) {
        this.deckId = deckId;
        this.tenantId = tenantId;
        this.sourceType = sourceType != null ? sourceType : "MANUAL";
        this.sourceId = sourceId;
        this.cardType = cardType;
        this.frontContent = frontContent;
        this.backContent = backContent;
        this.bloomLevel = bloomLevel;
        this.easinessFactor = 2.5;
        this.intervalDays = 0;
        this.repetitions = 0;
        this.lapses = 0;
        this.dueDate = Instant.now();
        this.status = "new";
    }

    public void update(String frontContent, String backContent, String cardType) {
        if (frontContent != null) this.frontContent = frontContent;
        if (backContent != null)  this.backContent = backContent;
        if (cardType != null)     this.cardType = cardType;
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
    }

    public void updateSrsFields(double easinessFactor, int intervalDays,
            int repetitions, int lapses, Instant dueDate) {
        this.easinessFactor = easinessFactor;
        this.intervalDays = intervalDays;
        this.repetitions = repetitions;
        this.lapses = lapses;
        this.dueDate = dueDate;
        this.lastReviewedAt = Instant.now();
        this.status = (repetitions == 0) ? "learning" : "review";
    }
}