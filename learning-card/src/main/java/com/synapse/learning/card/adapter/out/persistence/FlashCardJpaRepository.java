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

    Optional<FlashCard> findByIdAndDeletedAtIsNull(UUID id);

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
