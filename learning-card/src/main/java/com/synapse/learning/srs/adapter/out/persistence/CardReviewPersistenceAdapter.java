package com.synapse.learning.srs.adapter.out.persistence;

import com.synapse.learning.srs.application.port.out.CardReviewPort;
import com.synapse.learning.srs.domain.model.CardReview;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CardReviewPersistenceAdapter implements CardReviewPort {

    private final CardReviewJpaRepository jpaRepository;

    @Override
    public CardReview save(CardReview review) {
        return jpaRepository.save(review);
    }

    @Override
    public List<CardReview> findAllByCardIdOrderByReviewedAtDesc(UUID cardId) {
        return jpaRepository.findAllByCardIdOrderByReviewedAtDesc(cardId);
    }
}
