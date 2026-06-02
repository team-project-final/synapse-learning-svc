package com.synapse.learning.srs.adapter.out.event;

import com.synapse.learning.ReviewCompleted;
import com.synapse.learning.Rating;
import com.synapse.learning.srs.application.port.out.CardReviewedEventPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class CardReviewedEventPublisher implements CardReviewedEventPort {

    static final String TOPIC = "learning.card.review-completed-v1";

    private final KafkaTemplate<String, ReviewCompleted> reviewCompletedKafkaTemplate;

    /**
     * 복습 완료 이벤트를 비동기 발행한다.
     * 파티션 키 = tenantId → 같은 테넌트의 이벤트 순서 보장
     */
    @Override
    public void publish(String userId, String tenantId, String cardId, int rating, String nextReviewAt) {
        ReviewCompleted event = ReviewCompleted.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setUserId(userId)
                .setTenantId(tenantId)
                .setCardId(cardId)
                .setRating(toRating(rating))
                .setNextReviewAt(nextReviewAt)
                .setReviewedAt(Instant.now().toString())
                .setOccurredAt(Instant.now().toEpochMilli())
                .build();

        ProducerRecord<String, ReviewCompleted> record =
                new ProducerRecord<>(TOPIC, tenantId, event);  // key = tenantId (파티션 키)

        CompletableFuture<SendResult<String, ReviewCompleted>> future =
                reviewCompletedKafkaTemplate.send(record);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("[Kafka] {} 발행 실패 — userId={}, cardId={}, error={}",
                        TOPIC, userId, cardId, ex.getMessage(), ex);
            } else {
                log.debug("[Kafka] {} 발행 성공 — userId={}, cardId={}, offset={}",
                        TOPIC, userId, cardId,
                        result.getRecordMetadata().offset());
            }
        });
    }

    private Rating toRating(int rating) {
        return switch (rating) {
            case 1 -> Rating.AGAIN;
            case 2 -> Rating.HARD;
            case 3 -> Rating.GOOD;
            case 4 -> Rating.EASY;
            default -> throw new IllegalArgumentException("유효하지 않은 rating: " + rating);
        };
    }
}
