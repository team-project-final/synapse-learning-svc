package com.synapse.learning.card.adapter.out.persistence;

import com.synapse.learning.card.application.port.out.CardDeckPort;
import com.synapse.learning.card.domain.model.CardDeck;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CardDeckPersistenceAdapter implements CardDeckPort {

    private final CardDeckJpaRepository jpaRepository;

    @Override
    public CardDeck save(CardDeck deck) {
        return jpaRepository.save(deck);
    }

    @Override
    public CardDeck saveAndFlush(CardDeck deck) {
        return jpaRepository.saveAndFlush(deck);
    }

    @Override
    public Optional<CardDeck> findByIdAndDeletedAtIsNull(UUID id) {
        return jpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public Page<CardDeck> findAllByUserIdAndDeletedAtIsNull(UUID userId, Pageable pageable) {
        return jpaRepository.findAllByUserIdAndDeletedAtIsNull(userId, pageable);
    }
}
