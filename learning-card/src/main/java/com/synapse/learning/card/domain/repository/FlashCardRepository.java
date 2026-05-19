package com.synapse.learning.card.domain.repository;

import com.synapse.learning.card.domain.model.FlashCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;

public interface FlashCardRepository extends JpaRepository<FlashCard, UUID> {

    List<FlashCard> findAllByDeckIdAndDeletedAtIsNull(UUID deckId);

    Optional<FlashCard> findByIdAndDeletedAtIsNull(UUID id);

    @Query("SELECT c FROM FlashCard c WHERE c.deckId = :deckId " +
            "AND c.deletedAt IS NULL " +
            "AND c.dueDate <= :now " +
            "AND c.status IN ('new','learning','review','relearning') " +
            "ORDER BY c.dueDate ASC")
    List<FlashCard> findDueCards(@Param("deckId") UUID deckId,
            @Param("now") Instant now,
            Pageable pageable);
}