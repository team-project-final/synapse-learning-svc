package com.synapse.learning.card.domain.model;

import com.synapse.learning.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "card_decks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardDeck extends BaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "color", length = 7)
    private String color;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Builder
    public CardDeck(UUID userId, String tenantId, String name, String description, String color) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.name = name;
        this.description = description;
        this.color = color != null ? color : "#4A90E2";
    }

    // 덱 정보 수정
    public void update(String name, String description, String color) {
        if (name != null)
            this.name = name;
        if (description != null)
            this.description = description;
        if (color != null)
            this.color = color;
    }

    // 소프트 삭제
    public void softDelete() {
        this.deletedAt = Instant.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}