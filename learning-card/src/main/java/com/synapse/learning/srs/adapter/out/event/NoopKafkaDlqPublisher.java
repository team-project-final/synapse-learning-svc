package com.synapse.learning.srs.adapter.out.event;

import com.synapse.learning.srs.application.port.out.KafkaDlqPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "synapse.kafka", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoopKafkaDlqPublisher implements KafkaDlqPort {

    @Override
    public void publish(String originalTopic, String partitionKey, String payload) {
        log.debug("[Kafka disabled] DLQ 미발행 — originalTopic={}", originalTopic);
    }
}
