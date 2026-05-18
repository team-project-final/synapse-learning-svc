package com.synapse.learning.card.domain.model;

import com.synapse.learning.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cards")
@Getter
@NoArgsConstructor
public class FlashCard extends BaseEntity {

    @Column(name = "deck_id", nullable = false, updatable = false)
    private UUID deckId;

    @Column(name = "source_type", nullable = false, length = 20)
    private String sourceType;

    @Column(name = "source_id")
    private UUID sourceId;

    @Column(name = "card_type", nullable = false, length = 20)
    private String cardType;

    @Column(name = "front", nullable = false, columnDefinition = "TEXT")
    private String front;

    @Column(name = "back", nullable = false, columnDefinition = "TEXT")
    private String back;

    @Column(name = "bloom_level", length = 20)
    private String bloomLevel;

    @Column(name = "srs_algorithm", nullable = false, length = 20)
    private String srsAlgorithm;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "srs_state", nullable = false, columnDefinition = "jsonb")
    private String srsState;

    @Column(name = "next_review_at", nullable = false)
    private Instant nextReviewAt;

    @Column(name = "last_reviewed_at")
    private Instant lastReviewedAt;

    @Column(name = "state", nullable = false, length = 20)
    private String state;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extra", nullable = false, columnDefinition = "jsonb")
    private String extra;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Builder
    public FlashCard(UUID deckId, String tenantId, String sourceType, UUID sourceId,
            String cardType, String front, String back, String bloomLevel) {
        this.deckId = deckId;
        this.tenantId = tenantId;
        this.sourceType = sourceType != null ? sourceType : "MANUAL";
        this.sourceId = sourceId;
        this.cardType = cardType;
        this.front = front;
        this.back = back;
        this.bloomLevel = bloomLevel;
        this.srsAlgorithm = "SM2";
        this.srsState = "{\"easeFactor\":2.5,\"intervalDays\":0,\"repetitions\":0,\"lapses\":0}";
        this.nextReviewAt = Instant.now();
        this.state = "new";
        this.extra = "{}";
    }

    public void update(String front, String back, String cardType) {
        if (front != null)
            this.front = front;
        if (back != null)
            this.back = back;
        if (cardType != null)
            this.cardType = cardType;
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
    }
}