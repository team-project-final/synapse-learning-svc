package com.synapse.learning.card.adapter.out.persistence;

import com.synapse.learning.card.application.port.out.FlashCardPort;
import com.synapse.learning.card.domain.model.FlashCard;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FlashCardPersistenceAdapter implements FlashCardPort {

    private final FlashCardJpaRepository jpaRepository;

    @Override
    public FlashCard save(FlashCard card) {
        return jpaRepository.save(card);
    }

    @Override
    public FlashCard saveAndFlush(FlashCard card) {
        return jpaRepository.saveAndFlush(card);
    }

    @Override
    public Optional<FlashCard> findByIdAndDeletedAtIsNull(UUID id) {
        return jpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public Page<FlashCard> findAllByDeckIdAndDeletedAtIsNull(UUID deckId, Pageable pageable) {
        return jpaRepository.findAllByDeckIdAndDeletedAtIsNull(deckId, pageable);
    }

    @Override
    public List<FlashCard> findDueCards(UUID deckId, Instant now, Pageable pageable) {
        return jpaRepository.findDueCards(deckId, now, pageable);
    }
}
