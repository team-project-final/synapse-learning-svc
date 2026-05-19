package com.synapse.learning.srs.application.port.out;

import com.synapse.learning.srs.domain.model.CardReview;

import java.util.List;
import java.util.UUID;

public interface CardReviewPort {

    CardReview save(CardReview review);

    List<CardReview> findAllByCardIdOrderByReviewedAtDesc(UUID cardId);
}
