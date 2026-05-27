package com.synapse.learning.srs.adapter.out.persistence;

import com.synapse.learning.srs.domain.model.ReviewSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReviewSessionJpaRepository extends JpaRepository<ReviewSession, UUID> {

    Optional<ReviewSession> findByIdAndTenantId(UUID id, UUID tenantId);
}
