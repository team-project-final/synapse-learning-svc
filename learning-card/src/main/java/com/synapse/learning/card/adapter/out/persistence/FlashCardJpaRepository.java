package com.synapse.learning.card.adapter.out.persistence;

import com.synapse.learning.card.domain.model.FlashCard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FlashCardJpaRepository extends JpaRepository<FlashCard, UUID> {

    Page<FlashCard> findAllByDeckIdAndDeletedAtIsNull(UUID deckId, Pageable pageable);

    List<FlashCard> findAllByDeckIdAndDeletedAtIsNull(UUID deckId);

    Optional<FlashCard> findByIdAndDeletedAtIsNull(UUID id);

    @Query("""
            SELECT COUNT(c) > 0
            FROM FlashCard c
            JOIN CardDeck d ON d.id = c.deckId
            WHERE c.id = :cardId
              AND c.deletedAt IS NULL
              AND d.deletedAt IS NULL
              AND d.userId = :userId
              AND d.tenantId = :tenantId
            """)
    boolean existsActiveCardOwnedBy(@Param("cardId") UUID cardId,
            @Param("userId") UUID userId,
            @Param("tenantId") UUID tenantId);

    // ── 스케줄러용: 복습 대상 사용자 목록 조회 (userId + 카드 수) ──────────────
    @Query(value = """
            SELECT d.user_id AS userId, d.tenant_id AS tenantId, COUNT(*) AS dueCardCount
            FROM cards c
            JOIN card_decks d ON d.id = c.deck_id
            WHERE c.deleted_at IS NULL
              AND c.due_date <= NOW()
              AND c.status IN ('new', 'learning', 'review', 'relearning')
            GROUP BY d.user_id, d.tenant_id
            HAVING COUNT(*) > 0
            ORDER BY d.user_id
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<Object[]> findDueCardCountByUser(@Param("limit") int limit,
                                          @Param("offset") int offset);

    @Query("SELECT c FROM FlashCard c WHERE c.tenantId = :tenantId " +
            "AND c.deckId = :deckId " +
            "AND c.deletedAt IS NULL " +
            "AND c.dueDate <= :now " +
            "AND c.status IN ('new','learning','review','relearning') " +
            "ORDER BY c.dueDate ASC")
    List<FlashCard> findDueCards(@Param("tenantId") UUID tenantId,
            @Param("deckId") UUID deckId,
            @Param("now") Instant now,
            Pageable pageable);
}
