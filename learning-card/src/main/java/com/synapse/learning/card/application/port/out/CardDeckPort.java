package com.synapse.learning.card.application.port.out;

import com.synapse.learning.card.domain.model.CardDeck;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface CardDeckPort {

    CardDeck save(CardDeck deck);

    CardDeck saveAndFlush(CardDeck deck);

    Optional<CardDeck> findByIdAndDeletedAtIsNull(UUID id);

    Page<CardDeck> findAllByUserIdAndDeletedAtIsNull(UUID userId, Pageable pageable);
}
