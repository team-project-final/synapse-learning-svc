package com.synapse.learning.srs.adapter.out.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaDlqPublisherTest {

    @Mock
    KafkaTemplate<String, String> dlqKafkaTemplate;

    KafkaDlqPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new KafkaDlqPublisher(dlqKafkaTemplate);
    }

    @Test
    @DisplayName("publish 호출 시 원본 토픽 기반 DLQ 토픽으로 메시지가 전송된다")
    void publish_originalTopic_shouldSendToOriginalTopicDlq() {
        when(dlqKafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publish("learning.card.review-completed-v1", "tenant-001", "{\"error\":\"test\"}");

        verify(dlqKafkaTemplate).send(
                eq("learning.card.review-completed-v1.dlq"),
                eq("tenant-001"),
                anyString()
        );
    }
}
