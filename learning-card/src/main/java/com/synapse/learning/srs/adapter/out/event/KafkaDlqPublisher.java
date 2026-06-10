package com.synapse.learning.srs.adapter.out.event;

import com.synapse.learning.srs.application.port.out.KafkaDlqPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "synapse.kafka", name = "enabled", havingValue = "true")
public class KafkaDlqPublisher implements KafkaDlqPort {

    static String dlqTopic(String originalTopic) {
        return originalTopic + ".dlq";
    }

    private final KafkaTemplate<String, String> dlqKafkaTemplate;

    @Override
    public void publish(String originalTopic, String partitionKey, String payload) {
        String dlqTopic = dlqTopic(originalTopic);
        dlqKafkaTemplate.send(dlqTopic, partitionKey, payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[DLQ] DLQ 발행 실패 — originalTopic={}, dlqTopic={}, error={}",
                                originalTopic, dlqTopic, ex.getMessage());
                    } else {
                        log.warn("[DLQ] {} 실패 이벤트 보관 완료 — dlqTopic={}, key={}",
                                originalTopic, dlqTopic, partitionKey);
                    }
                });
    }
}
