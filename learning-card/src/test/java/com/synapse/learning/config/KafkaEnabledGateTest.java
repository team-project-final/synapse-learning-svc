package com.synapse.learning.config;

import com.synapse.learning.srs.adapter.out.event.CardReviewedEventPublisher;
import com.synapse.learning.srs.adapter.out.event.KafkaDlqPublisher;
import com.synapse.learning.srs.adapter.out.event.NoopCardReviewedEventPublisher;
import com.synapse.learning.srs.adapter.out.event.NoopKafkaDlqPublisher;
import com.synapse.learning.srs.adapter.out.event.NoopReviewDueEventPublisher;
import com.synapse.learning.srs.adapter.out.event.ReviewDueEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaEnabledGateTest {

    @Nested
    @DisplayName("synapse.kafka.enabled=true 일 때")
    class WhenEnabled {

        final ApplicationContextRunner runner = new ApplicationContextRunner()
                .withUserConfiguration(KafkaTopicProperties.class, KafkaConfig.class, KafkaDlqPublisher.class, CardReviewedEventPublisher.class, ReviewDueEventPublisher.class)
                .withPropertyValues(
                        "synapse.kafka.enabled=true",
                        "spring.kafka.bootstrap-servers=localhost:9092",
                        "spring.kafka.security.protocol=PLAINTEXT",
                        "spring.kafka.producer.properties.schema.registry.url=mock://test",
                        "spring.kafka.producer.acks=all",
                        "spring.kafka.producer.retries=3",
                        "spring.kafka.producer.properties.retry.backoff.ms=1000",
                        "spring.kafka.consumer.properties.schema.registry.url=mock://test"
                );

        @Test
        @DisplayName("KafkaConfig Bean이 생성된다")
        void kafkaConfigBeanExists() {
            runner.run(ctx -> assertThat(ctx).hasSingleBean(KafkaConfig.class));
        }

        @Test
        @DisplayName("DLQ Publisher Bean이 생성된다")
        void dlqPublisherExists() {
            runner.run(ctx -> assertThat(ctx).hasSingleBean(KafkaDlqPublisher.class));
        }

        @Test
        @DisplayName("Noop Publisher Bean이 생성되지 않는다")
        void noopPublishersAbsent() {
            runner.run(ctx -> {
                assertThat(ctx).doesNotHaveBean(NoopCardReviewedEventPublisher.class);
                assertThat(ctx).doesNotHaveBean(NoopReviewDueEventPublisher.class);
                assertThat(ctx).doesNotHaveBean(NoopKafkaDlqPublisher.class);
            });
        }
    }

    @Nested
    @DisplayName("synapse.kafka.enabled 미설정(기본값 false) 일 때")
    class WhenDisabled {

        final ApplicationContextRunner runner = new ApplicationContextRunner()
                .withUserConfiguration(
                        KafkaConfig.class,
                        NoopKafkaDlqPublisher.class,
                        NoopCardReviewedEventPublisher.class,
                        NoopReviewDueEventPublisher.class
                );

        @Test
        @DisplayName("실제 Publisher Bean이 생성되지 않는다")
        void realPublishersAbsent() {
            runner.run(ctx -> {
                assertThat(ctx).doesNotHaveBean(CardReviewedEventPublisher.class);
                assertThat(ctx).doesNotHaveBean(ReviewDueEventPublisher.class);
                assertThat(ctx).doesNotHaveBean(KafkaDlqPublisher.class);
            });
        }

        @Test
        @DisplayName("Noop Publisher Bean이 생성된다")
        void noopPublishersExist() {
            runner.run(ctx -> {
                assertThat(ctx).hasSingleBean(NoopCardReviewedEventPublisher.class);
                assertThat(ctx).hasSingleBean(NoopReviewDueEventPublisher.class);
                assertThat(ctx).hasSingleBean(NoopKafkaDlqPublisher.class);
            });
        }
    }
}
