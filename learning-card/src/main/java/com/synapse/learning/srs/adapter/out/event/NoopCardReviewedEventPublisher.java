package com.synapse.learning.srs.adapter.out.event;

import com.synapse.learning.srs.application.port.out.CardReviewedEventPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "synapse.kafka", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoopCardReviewedEventPublisher implements CardReviewedEventPort {

    @Override
    public void publish(String userId, String tenantId, String cardId, int rating, String nextReviewAt) {
        log.debug("[Kafka disabled] card.reviewed 미발행 — userId={}, cardId={}", userId, cardId);
    }
}
