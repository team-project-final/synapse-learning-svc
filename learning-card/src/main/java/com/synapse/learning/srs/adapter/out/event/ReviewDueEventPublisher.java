package com.synapse.learning.srs.adapter.out.event;

import com.synapse.learning.event.CardReviewDue;
import com.synapse.learning.srs.application.port.out.ReviewDueEventPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewDueEventPublisher implements ReviewDueEventPort {

    static final String TOPIC = "card.review.due";

    private final KafkaTemplate<String, CardReviewDue> reviewDueKafkaTemplate;

    /**
     * 복습 리마인더 이벤트를 비동기 발행한다.
     * 파티션 키 = userId → 같은 사용자의 이벤트 순서 보장
     */
    @Override
    public void publish(String userId, int dueCardCount, String dueDate) {
        CardReviewDue event = CardReviewDue.newBuilder()
                .setUserId(userId)
                .setDueCardCount(dueCardCount)
                .setDueDate(dueDate)
                .setOccurredAt(Instant.now())
                .build();

        ProducerRecord<String, CardReviewDue> record =
                new ProducerRecord<>(TOPIC, userId, event);  // key = userId (파티션 키)

        CompletableFuture<SendResult<String, CardReviewDue>> future =
                kafkaTemplate().send(record);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("[Kafka] card.review.due 발행 실패 — userId={}, dueCardCount={}, error={}",
                        userId, dueCardCount, ex.getMessage(), ex);
            } else {
                log.debug("[Kafka] card.review.due 발행 성공 — userId={}, dueDate={}, offset={}",
                        userId, dueDate,
                        result.getRecordMetadata().offset());
            }
        });
    }

    private KafkaTemplate<String, CardReviewDue> kafkaTemplate() {
        return reviewDueKafkaTemplate;
    }
}
