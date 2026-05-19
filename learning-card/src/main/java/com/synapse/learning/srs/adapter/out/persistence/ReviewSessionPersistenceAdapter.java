package com.synapse.learning.srs.adapter.out.persistence;

import com.synapse.learning.srs.application.port.out.ReviewSessionPort;
import com.synapse.learning.srs.domain.model.ReviewSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReviewSessionPersistenceAdapter implements ReviewSessionPort {

    private final ReviewSessionJpaRepository jpaRepository;

    @Override
    public ReviewSession save(ReviewSession session) {
        return jpaRepository.save(session);
    }

    @Override
    public Optional<ReviewSession> findByIdAndTenantId(UUID id, UUID tenantId) {
        return jpaRepository.findByIdAndTenantId(id, tenantId);
    }
}
