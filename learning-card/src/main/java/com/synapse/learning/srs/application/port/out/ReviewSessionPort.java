package com.synapse.learning.srs.application.port.out;

import com.synapse.learning.srs.domain.model.ReviewSession;

import java.util.Optional;
import java.util.UUID;

public interface ReviewSessionPort {

    ReviewSession save(ReviewSession session);

    Optional<ReviewSession> findByIdAndTenantId(UUID id, UUID tenantId);
}
