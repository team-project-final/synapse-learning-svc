package com.synapse.learning.srs.adapter.out.event;

import com.synapse.event.learning.CardReviewDue;
import com.synapse.learning.srs.application.port.out.KafkaDlqPort;
import com.synapse.learning.srs.application.port.out.ReviewDueEventPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "synapse.kafka", name = "enabled", havingValue = "true")
public class ReviewDueEventPublisher implements ReviewDueEventPort {

    static final String TOPIC = "learning.card.review-due-v1";

    private final KafkaTemplate<String, CardReviewDue> reviewDueKafkaTemplate;
    private final KafkaDlqPort kafkaDlqPort;

    @Override
    public void publish(String userId, String tenantId, int dueCardCount, String dueDate) {
        Instant now = Instant.now();
        CardReviewDue event = CardReviewDue.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setUserId(userId)
                .setTenantId(tenantId)
                .setDueCardCount(dueCardCount)
                .setDueDate(dueDate)
                .setOccurredAt(now)
                .build();

        ProducerRecord<String, CardReviewDue> record =
                new ProducerRecord<>(TOPIC, userId, event);

        CompletableFuture<SendResult<String, CardReviewDue>> future =
                reviewDueKafkaTemplate.send(record);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("[Kafka] {} 발행 실패 — userId={}, dueCardCount={}, error={}",
                        TOPIC, userId, dueCardCount, ex.getMessage(), ex);
                String payload = String.format(
                        "{\"topic\":\"%s\",\"userId\":\"%s\",\"dueCardCount\":%d,\"dueDate\":\"%s\",\"error\":\"%s\"}",
                        TOPIC, userId, dueCardCount, dueDate, ex.getMessage());
                kafkaDlqPort.publish(TOPIC, tenantId, payload);
            } else {
                log.debug("[Kafka] {} 발행 성공 — userId={}, dueDate={}, offset={}",
                        TOPIC, userId, dueDate,
                        result.getRecordMetadata().offset());
            }
        });
    }
}
