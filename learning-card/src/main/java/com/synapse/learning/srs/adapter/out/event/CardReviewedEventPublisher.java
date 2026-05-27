package com.synapse.learning.srs.adapter.out.event;

import com.synapse.learning.event.CardReviewed;
import com.synapse.learning.event.Rating;
import com.synapse.learning.srs.application.port.out.CardReviewedEventPort;
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
public class CardReviewedEventPublisher implements CardReviewedEventPort {

    static final String TOPIC = "card.reviewed";

    private final KafkaTemplate<String, CardReviewed> kafkaTemplate;

    /**
     * 복습 완료 이벤트를 비동기 발행한다.
     * 파티션 키 = userId → 같은 사용자의 이벤트 순서 보장
     */
    @Override
    public void publish(String userId, String cardId, String deckId, int rating) {
        CardReviewed event = CardReviewed.newBuilder()
                .setUserId(userId)
                .setCardId(cardId)
                .setDeckId(deckId)
                .setRating(toRating(rating))
                .setReviewedAt(Instant.now())
                .build();

        ProducerRecord<String, CardReviewed> record =
                new ProducerRecord<>(TOPIC, userId, event);  // key = userId (파티션 키)

        CompletableFuture<SendResult<String, CardReviewed>> future =
                kafkaTemplate.send(record);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("[Kafka] card.reviewed 발행 실패 — userId={}, cardId={}, error={}",
                        userId, cardId, ex.getMessage(), ex);
            } else {
                log.debug("[Kafka] card.reviewed 발행 성공 — userId={}, cardId={}, offset={}",
                        userId, cardId,
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
