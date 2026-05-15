package com.synapse.learning.card.domain.repository;

import com.synapse.learning.card.domain.model.CardDeck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CardDeckRepository extends JpaRepository<CardDeck, UUID> {

    // 삭제되지 않은 내 덱 전체 조회
    List<CardDeck> findAllByUserIdAndDeletedAtIsNull(UUID userId);

    // 삭제되지 않은 특정 덱 조회
    Optional<CardDeck> findByIdAndDeletedAtIsNull(UUID id);
}