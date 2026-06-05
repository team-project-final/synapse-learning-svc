package com.synapse.learning.config;

import com.synapse.learning.CardReviewDue;
import com.synapse.learning.ReviewCompleted;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaConfigSecurityProtocolTest {

    @Nested
    @ExtendWith(SpringExtension.class)
    @Import(KafkaConfig.class)
    @TestPropertySource(properties = {
            "synapse.kafka.enabled=true",
            "spring.kafka.bootstrap-servers=localhost:9094",
            "spring.kafka.producer.properties.schema.registry.url=mock://test",
            "spring.kafka.producer.acks=all",
            "spring.kafka.producer.retries=3",
            "spring.kafka.producer.properties.retry.backoff.ms=1000",
            "spring.kafka.consumer.properties.schema.registry.url=mock://test",
            "spring.kafka.security.protocol=SSL"
    })
    @DisplayName("SSL 주입 시")
    class WhenSslInjected {

        @Autowired
        ProducerFactory<String, ReviewCompleted> reviewCompletedProducerFactory;

        @Autowired
        ProducerFactory<String, CardReviewDue> reviewDueProducerFactory;

        @Autowired
        ConsumerFactory<String, Object> consumerFactory;

        @Test
        @DisplayName("producer·consumer factory 모두 security.protocol=SSL 이 설정된다")
        void securityProtocolIsSSL() {
            assertThat(reviewCompletedProducerFactory.getConfigurationProperties())
                    .containsEntry("security.protocol", "SSL");
            assertThat(reviewDueProducerFactory.getConfigurationProperties())
                    .containsEntry("security.protocol", "SSL");
            assertThat(consumerFactory.getConfigurationProperties())
                    .containsEntry("security.protocol", "SSL");
        }
    }

    @Nested
    @ExtendWith(SpringExtension.class)
    @Import(KafkaConfig.class)
    @TestPropertySource(properties = {
            "synapse.kafka.enabled=true",
            "spring.kafka.bootstrap-servers=localhost:9092",
            "spring.kafka.producer.properties.schema.registry.url=mock://test",
            "spring.kafka.producer.acks=all",
            "spring.kafka.producer.retries=3",
            "spring.kafka.producer.properties.retry.backoff.ms=1000",
            "spring.kafka.consumer.properties.schema.registry.url=mock://test"
    })
    @DisplayName("security.protocol 미주입(PLAINTEXT 기본값) 시")
    class WhenPlaintextDefault {

        @Autowired
        ProducerFactory<String, ReviewCompleted> reviewCompletedProducerFactory;

        @Autowired
        ProducerFactory<String, CardReviewDue> reviewDueProducerFactory;

        @Autowired
        ConsumerFactory<String, Object> consumerFactory;

        @Test
        @DisplayName("security.protocol 키가 factory props에 존재하지 않는다")
        void securityProtocolKeyAbsent() {
            assertThat(reviewCompletedProducerFactory.getConfigurationProperties())
                    .doesNotContainKey("security.protocol");
            assertThat(reviewDueProducerFactory.getConfigurationProperties())
                    .doesNotContainKey("security.protocol");
            assertThat(consumerFactory.getConfigurationProperties())
                    .doesNotContainKey("security.protocol");
        }
    }
}
