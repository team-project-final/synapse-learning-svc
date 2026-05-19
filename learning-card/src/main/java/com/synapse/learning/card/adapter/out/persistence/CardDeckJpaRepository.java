package com.synapse.learning.card.adapter.out.persistence;

import com.synapse.learning.card.domain.model.CardDeck;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CardDeckJpaRepository extends JpaRepository<CardDeck, UUID> {

    Page<CardDeck> findAllByUserIdAndDeletedAtIsNull(UUID userId, Pageable pageable);

    Optional<CardDeck> findByIdAndDeletedAtIsNull(UUID id);
}
