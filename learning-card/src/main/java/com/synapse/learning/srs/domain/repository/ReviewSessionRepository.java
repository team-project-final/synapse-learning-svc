package com.synapse.learning.srs.domain.repository;

import com.synapse.learning.srs.domain.model.ReviewSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReviewSessionRepository extends JpaRepository<ReviewSession, UUID> {

    Optional<ReviewSession> findByIdAndTenantId(UUID id, UUID tenantId);
}