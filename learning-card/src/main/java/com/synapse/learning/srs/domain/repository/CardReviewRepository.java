package com.synapse.learning.srs.domain.repository;

import com.synapse.learning.srs.domain.model.CardReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CardReviewRepository extends JpaRepository<CardReview, Long> {

    java.util.List<CardReview> findAllByCardIdOrderByReviewedAtDesc(UUID cardId);
}