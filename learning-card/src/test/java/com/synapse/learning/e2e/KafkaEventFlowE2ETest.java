package com.synapse.learning.e2e;

import com.synapse.event.learning.CardReviewDue;
import com.synapse.event.learning.Rating;
import com.synapse.event.learning.ReviewCompleted;
import com.synapse.learning.srs.adapter.out.event.CardReviewedEventPublisher;
import com.synapse.learning.srs.adapter.out.event.ReviewDueEventPublisher;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kafka 이벤트 플로우 E2E — EmbeddedKafka + Avro 역직렬화까지 검증
 *
 * 기존 CardReviewedEventPublisherIntegrationTest 는 바이트 수신만 확인.
 * 이 테스트는 실제 Avro 역직렬화 후 각 필드값까지 검증한다.
 *
 * 시나리오 1: card.reviewed  — ReviewCompleted 이벤트 필드 검증
 * 시나리오 2: card.review.due — CardReviewDue    이벤트 필드 검증
 * 시나리오 3: 파티션 키 = userId 보장 확인
 */
@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        topics = {
                KafkaEventFlowE2ETest.REVIEW_COMPLETED_TOPIC,
                KafkaEventFlowE2ETest.REVIEW_DUE_TOPIC
        }
)
@TestPropertySource(properties = {
        "synapse.kafka.enabled=true",
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.producer.properties.schema.registry.url=mock://e2e-scope",
        "spring.kafka.consumer.properties.schema.registry.url=mock://e2e-scope",
        "spring.main.allow-bean-definition-overriding=true",
        // @DirtiesContext 종료 시 create-drop 이 공유 H2(mem:learning) 테이블을 DROP 하는 것을 방지
        // 이 테스트 전용 DB 를 사용해 다른 테스트 컨텍스트의 스키마를 보호한다
        "spring.datasource.url=jdbc:h2:mem:kafka-e2e-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
@DirtiesContext
@DisplayName("[E2E] Kafka 이벤트 플로우 — Avro 필드 검증")
class KafkaEventFlowE2ETest {

    static final String REVIEW_COMPLETED_TOPIC = "learning.card.review-completed-v1";
    static final String REVIEW_DUE_TOPIC       = "learning.card.review-due-v1";
    private static final String MOCK_REGISTRY  = "mock://e2e-scope";

    // ── TestConfiguration: KafkaConfig 빈을 EmbeddedKafka + mock registry 로 교체 ──

    @TestConfiguration
    static class KafkaTestConfig {

        @Bean
        @Primary
        KafkaTemplate<String, ReviewCompleted> reviewCompletedKafkaTemplate(
                @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
            return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(avroProducerProps(bootstrapServers)));
        }

        @Bean
        @Primary
        KafkaTemplate<String, CardReviewDue> reviewDueKafkaTemplate(
                @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
            return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(avroProducerProps(bootstrapServers)));
        }

        @Bean
        @Primary
        KafkaTemplate<String, String> dlqKafkaTemplate(
                @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
            Map<String, Object> props = new HashMap<>();
            props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringSerializer.class);
            return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
        }

        private Map<String, Object> avroProducerProps(String bootstrapServers) {
            Map<String, Object> props = new HashMap<>();
            props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
            props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, MOCK_REGISTRY);
            return props;
        }
    }

    // ── 주입 ──

    @Autowired
    CardReviewedEventPublisher cardReviewedEventPublisher;

    @Autowired
    ReviewDueEventPublisher reviewDueEventPublisher;

    @Autowired
    EmbeddedKafkaBroker embeddedKafkaBroker;

    // ── 시나리오 1: ReviewCompleted ──

    @Test
    @DisplayName("복습 완료 시 ReviewCompleted Avro 이벤트가 올바른 필드로 토픽에 발행된다")
    void reviewCompleted_발행시_Avro필드가_올바르게_직렬화된다() {
        String userId     = UUID.randomUUID().toString();
        String tenantId   = UUID.randomUUID().toString();
        String cardId     = UUID.randomUUID().toString();
        String nextReview = "2026-06-09";

        cardReviewedEventPublisher.publish(userId, tenantId, cardId, 3, nextReview);

        Consumer<String, ReviewCompleted> consumer = createAvroConsumer("rc-group-" + userId);
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, REVIEW_COMPLETED_TOPIC);

        ConsumerRecords<String, ReviewCompleted> records =
                KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));
        consumer.close();

        assertThat(records.count()).isGreaterThan(0);
        ReviewCompleted event = records.iterator().next().value();
        assertThat(event.getCardId().toString()).isEqualTo(cardId);
        assertThat(event.getUserId().toString()).isEqualTo(userId);
        assertThat(event.getTenantId().toString()).isEqualTo(tenantId);
        assertThat(event.getRating()).isEqualTo(Rating.GOOD);
        assertThat(event.getNextReviewAt().toString()).isEqualTo(nextReview);
        assertThat(event.getEventId().toString()).isNotBlank();
        assertThat(event.getOccurredAt()).isNotNull();
    }

    // ── 시나리오 2: CardReviewDue ──

    @Test
    @DisplayName("복습 리마인더 발행 시 CardReviewDue Avro 이벤트가 올바른 필드로 토픽에 발행된다")
    void cardReviewDue_발행시_Avro필드가_올바르게_직렬화된다() {
        String userId   = UUID.randomUUID().toString();
        String tenantId = UUID.randomUUID().toString();
        int dueCount    = 7;
        String dueDate  = "2026-06-08";

        reviewDueEventPublisher.publish(userId, tenantId, dueCount, dueDate);

        Consumer<String, CardReviewDue> consumer = createAvroConsumer("rd-group-" + userId);
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, REVIEW_DUE_TOPIC);

        ConsumerRecords<String, CardReviewDue> records =
                KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));
        consumer.close();

        assertThat(records.count()).isGreaterThan(0);
        CardReviewDue event = records.iterator().next().value();
        assertThat(event.getUserId().toString()).isEqualTo(userId);
        assertThat(event.getTenantId().toString()).isEqualTo(tenantId);
        assertThat(event.getDueCardCount()).isEqualTo(dueCount);
        assertThat(event.getDueDate().toString()).isEqualTo(dueDate);
        assertThat(event.getEventId().toString()).isNotBlank();
        assertThat(event.getOccurredAt()).isNotNull();
    }

    // ── 시나리오 3: 파티션 키 ──

    @Test
    @DisplayName("ReviewCompleted 이벤트의 파티션 키는 userId 이다 (사용자 순서 보장)")
    void reviewCompleted_파티션키가_userId다() {
        String userId = UUID.randomUUID().toString();

        cardReviewedEventPublisher.publish(userId, "tenant-1",
                UUID.randomUUID().toString(), 4, "2026-06-10");

        // 동일 EmbeddedKafka 내 이전 테스트 레코드도 earliest로 읽히므로
        // 해당 userId 키를 가진 레코드가 존재하는지 확인한다
        Consumer<String, ReviewCompleted> consumer = createAvroConsumer("pk-group-" + userId);
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, REVIEW_COMPLETED_TOPIC);

        ConsumerRecords<String, ReviewCompleted> records =
                KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));
        consumer.close();

        boolean found = false;
        for (var record : records) {
            if (userId.equals(record.key())) {
                found = true;
                break;
            }
        }
        assertThat(found).as("파티션 키 userId=%s 를 가진 레코드가 없음", userId).isTrue();
    }

    // ── 헬퍼 ──

    @SuppressWarnings("unchecked")
    private <V> Consumer<String, V> createAvroConsumer(String groupId) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "10");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, MOCK_REGISTRY);
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
        DefaultKafkaConsumerFactory<String, Object> factory = new DefaultKafkaConsumerFactory<>(props);
        return (Consumer<String, V>) factory.createConsumer();
    }
}
