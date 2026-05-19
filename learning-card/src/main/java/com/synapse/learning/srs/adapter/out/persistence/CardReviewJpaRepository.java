package com.synapse.learning.srs.adapter.out.persistence;

import com.synapse.learning.srs.domain.model.CardReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CardReviewJpaRepository extends JpaRepository<CardReview, UUID> {

    List<CardReview> findAllByCardIdOrderByReviewedAtDesc(UUID cardId);
}
