package com.synapse.learning.card.domain.repository;

import com.synapse.learning.card.domain.model.FlashCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FlashCardRepository extends JpaRepository<FlashCard, UUID> {

    List<FlashCard> findAllByDeckIdAndDeletedAtIsNull(UUID deckId);

    Optional<FlashCard> findByIdAndDeletedAtIsNull(UUID id);
}