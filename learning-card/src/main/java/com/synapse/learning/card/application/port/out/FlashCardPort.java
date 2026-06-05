package com.synapse.learning.card.application.port.out;

import com.synapse.learning.card.domain.model.FlashCard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FlashCardPort {

    FlashCard save(FlashCard card);

    FlashCard saveAndFlush(FlashCard card);

    Optional<FlashCard> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsActiveCardOwnedBy(UUID cardId, UUID userId, UUID tenantId);

    Page<FlashCard> findAllByDeckIdAndDeletedAtIsNull(UUID deckId, Pageable pageable);

    List<FlashCard> findDueCards(UUID tenantId, UUID deckId, Instant now, Pageable pageable);
}
