package com.synapse.learning.srs.adapter.out.event;

import com.synapse.learning.event.CardReviewed;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CardReviewedEventPublisherTest {

    @Mock
    KafkaTemplate<String, CardReviewed> kafkaTemplate;

    @InjectMocks
    CardReviewedEventPublisher publisher;

    private static final String USER_ID = UUID.randomUUID().toString();
    private static final String CARD_ID = UUID.randomUUID().toString();
    private static final String DECK_ID = UUID.randomUUID().toString();

    @Test
    @DisplayName("rating=3(GOOD) 발행 시 card.reviewed 토픽으로 이벤트가 전송된다")
    void publish_good_sendsToTopic() {
        given(kafkaTemplate.send(any(ProducerRecord.class))).willReturn(
                CompletableFuture.completedFuture(
                        new SendResult<>(null, null)));

        publisher.publish(USER_ID, CARD_ID, DECK_ID, 3);

        ArgumentCaptor<org.apache.kafka.clients.producer.ProducerRecord<String, CardReviewed>> captor =
                ArgumentCaptor.forClass(org.apache.kafka.clients.producer.ProducerRecord.class);
        verify(kafkaTemplate).send(captor.capture());

        var record = captor.getValue();
        assertThat(record.topic()).isEqualTo(CardReviewedEventPublisher.TOPIC);
        assertThat(record.key()).isEqualTo(USER_ID);
        assertThat(record.value().getUserId()).isEqualTo(USER_ID);
        assertThat(record.value().getCardId()).isEqualTo(CARD_ID);
        assertThat(record.value().getRating().toString()).isEqualTo("GOOD");
    }

    @Test
    @DisplayName("rating=1(AGAIN) 발행 시 Rating.AGAIN으로 변환된다")
    void publish_again_setsRatingAgain() {
        given(kafkaTemplate.send(any(ProducerRecord.class))).willReturn(
                CompletableFuture.completedFuture(
                        new SendResult<>(null, null)));

        publisher.publish(USER_ID, CARD_ID, DECK_ID, 1);

        ArgumentCaptor<org.apache.kafka.clients.producer.ProducerRecord<String, CardReviewed>> captor =
                ArgumentCaptor.forClass(org.apache.kafka.clients.producer.ProducerRecord.class);
        verify(kafkaTemplate).send(captor.capture());

        assertThat(captor.getValue().value().getRating().toString()).isEqualTo("AGAIN");
    }

    @Test
    @DisplayName("발행 실패 시 예외를 전파하지 않고 에러 로그만 남긴다")
    void publish_failure_doesNotThrow() {
        CompletableFuture<SendResult<String, CardReviewed>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka 연결 실패"));
        given(kafkaTemplate.send(any(ProducerRecord.class))).willReturn(failedFuture);

        // 예외가 전파되지 않아야 함
        org.assertj.core.api.Assertions.assertThatCode(
                () -> publisher.publish(USER_ID, CARD_ID, DECK_ID, 3))
                .doesNotThrowAnyException();
    }
}
