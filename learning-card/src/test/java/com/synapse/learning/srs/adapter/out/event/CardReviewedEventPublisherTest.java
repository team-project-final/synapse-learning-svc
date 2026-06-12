package com.synapse.learning.srs.adapter.out.event;

import com.synapse.learning.ReviewCompleted;
import com.synapse.learning.config.KafkaTopicProperties;
import com.synapse.learning.srs.application.port.out.KafkaDlqPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.apache.kafka.clients.producer.ProducerRecord;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CardReviewedEventPublisherTest {

    @Mock
    KafkaTemplate<String, ReviewCompleted> reviewCompletedKafkaTemplate;

    @Mock
    KafkaDlqPort kafkaDlqPort;

    @Mock
    KafkaTopicProperties topicProperties;

    @InjectMocks
    CardReviewedEventPublisher publisher;

    private static final String USER_ID = UUID.randomUUID().toString();
    private static final String TENANT_ID = UUID.randomUUID().toString();
    private static final String CARD_ID = UUID.randomUUID().toString();
    private static final String NEXT_REVIEW_AT = "2026-06-08";

    @BeforeEach
    void setUp() {
        // 테스트에서는 prefix 없음 → base 토픽 그대로 반환
        given(topicProperties.resolve(anyString())).willAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("rating=3(GOOD) 발행 시 learning.card.review-completed-v1 토픽으로 이벤트가 전송된다")
    void publish_good_sendsToTopic() {
        given(reviewCompletedKafkaTemplate.send(any(ProducerRecord.class))).willReturn(
                CompletableFuture.completedFuture(
                        new SendResult<>(null, null)));

        publisher.publish(USER_ID, TENANT_ID, CARD_ID, 3, NEXT_REVIEW_AT);

        ArgumentCaptor<ProducerRecord<String, ReviewCompleted>> captor =
                ArgumentCaptor.forClass(ProducerRecord.class);
        verify(reviewCompletedKafkaTemplate).send(captor.capture());

        var record = captor.getValue();
        assertThat(record.topic()).isEqualTo(CardReviewedEventPublisher.TOPIC_BASE);
        assertThat(record.key()).isEqualTo(USER_ID);
        assertThat(record.value().getUserId()).isEqualTo(USER_ID);
        assertThat(record.value().getTenantId()).isEqualTo(TENANT_ID);
        assertThat(record.value().getCardId()).isEqualTo(CARD_ID);
        assertThat(record.value().getRating().toString()).isEqualTo("GOOD");
        assertThat(record.value().getNextReviewAt()).isEqualTo(NEXT_REVIEW_AT);
    }

    @Test
    @DisplayName("rating=1(AGAIN) 발행 시 Rating.AGAIN으로 변환된다")
    void publish_again_setsRatingAgain() {
        given(reviewCompletedKafkaTemplate.send(any(ProducerRecord.class))).willReturn(
                CompletableFuture.completedFuture(
                        new SendResult<>(null, null)));

        publisher.publish(USER_ID, TENANT_ID, CARD_ID, 1, NEXT_REVIEW_AT);

        ArgumentCaptor<ProducerRecord<String, ReviewCompleted>> captor =
                ArgumentCaptor.forClass(ProducerRecord.class);
        verify(reviewCompletedKafkaTemplate).send(captor.capture());

        assertThat(captor.getValue().value().getRating().toString()).isEqualTo("AGAIN");
    }

    @Test
    @DisplayName("발행 실패 시 예외를 전파하지 않고 에러 로그만 남긴다")
    void publish_failure_doesNotThrow() {
        CompletableFuture<SendResult<String, ReviewCompleted>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka 연결 실패"));
        given(reviewCompletedKafkaTemplate.send(any(ProducerRecord.class))).willReturn(failedFuture);

        org.assertj.core.api.Assertions.assertThatCode(
                () -> publisher.publish(USER_ID, TENANT_ID, CARD_ID, 3, NEXT_REVIEW_AT))
                .doesNotThrowAnyException();
    }
}
