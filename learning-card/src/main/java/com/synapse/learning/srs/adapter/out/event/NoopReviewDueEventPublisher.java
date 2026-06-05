package com.synapse.learning.srs.adapter.out.event;

import com.synapse.learning.srs.application.port.out.ReviewDueEventPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "synapse.kafka", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoopReviewDueEventPublisher implements ReviewDueEventPort {

    @Override
    public void publish(String userId, String tenantId, int dueCardCount, String dueDate) {
        log.debug("[Kafka disabled] card.review.due 미발행 — userId={}, dueCardCount={}", userId, dueCardCount);
    }
}
