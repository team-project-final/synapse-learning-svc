package com.synapse.learning.e2e;

import com.synapse.event.learning.CardReviewDue;
import com.synapse.learning.Rating;
import com.synapse.learning.ReviewCompleted;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 라이브 Kafka E2E — Testcontainers 실제 Kafka 컨테이너 + Avro 역직렬화 검증
 *
 * KafkaEventFlowE2ETest(EmbeddedKafka)와 동일한 시나리오를 실제 Docker 컨테이너로 검증.
 * engagement/platform 소비 측이 수신 가능한 Avro 페이로드임을 실제 네트워크 소켓으로 확인한다.
 *
 * 시나리오 1: ReviewCompleted — 실제 Kafka 컨테이너에서 발행·소비 + 필드 검증
 * 시나리오 2: CardReviewDue   — 실제 Kafka 컨테이너에서 발행·소비 + 필드 검증
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@DirtiesContext
@TestPropertySource(properties = {
        "synapse.kafka.enabled=true",
        "spring.kafka.producer.properties.schema.registry.url=mock://live-scope",
        "spring.kafka.consumer.properties.schema.registry.url=mock://live-scope",
        "spring.main.allow-bean-definition-overriding=true",
        // kafka-e2e-test 와 분리된 독립 DB — DirtiesContext 종료 시 다른 컨텍스트 영향 방지
        "spring.datasource.url=jdbc:h2:mem:kafka-live-e2e;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
@DisplayName("[E2E][Live] Testcontainers Kafka 컨테이너 기반 라이브 이벤트 발행 검증")
class KafkaLiveContainerE2ETest {

    static final String REVIEW_COMPLETED_TOPIC = "learning.card.review-completed-v1";
    static final String REVIEW_DUE_TOPIC       = "learning.card.review-due-v1";
    private static final String MOCK_REGISTRY  = "mock://live-scope";

    @Container
    static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @DynamicPropertySource
    static void overrideKafkaBootstrapServers(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    // ── TestConfiguration: KafkaConfig 빈을 컨테이너 Kafka + mock registry 로 교체 ──

    @TestConfiguration
    static class KafkaLiveTestConfig {

        @Bean @Primary
        KafkaTemplate<String, ReviewCompleted> reviewCompletedKafkaTemplate(
                @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
            return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(avroProducerProps(bootstrapServers)));
        }

        @Bean @Primary
        KafkaTemplate<String, CardReviewDue> reviewDueKafkaTemplate(
                @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
            return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(avroProducerProps(bootstrapServers)));
        }

        @Bean @Primary
        KafkaTemplate<String, String> dlqKafkaTemplate(
                @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
            Map<String, Object> props = new HashMap<>();
            props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
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

    @Autowired CardReviewedEventPublisher cardReviewedEventPublisher;
    @Autowired ReviewDueEventPublisher    reviewDueEventPublisher;

    // ── 시나리오 1: ReviewCompleted 라이브 컨테이너 검증 ──

    @Test
    @DisplayName("실제 Kafka 컨테이너에서 ReviewCompleted Avro 이벤트가 발행되고 소비된다")
    void reviewCompleted_라이브컨테이너에서_Avro필드가_검증된다() {
        String userId     = UUID.randomUUID().toString();
        String tenantId   = UUID.randomUUID().toString();
        String cardId     = UUID.randomUUID().toString();
        String nextReview = "2026-06-09";

        cardReviewedEventPublisher.publish(userId, tenantId, cardId, 3, nextReview);

        ConsumerRecords<String, ReviewCompleted> records = pollUntilNotEmpty(
                createAvroConsumer("rc-live-" + userId, REVIEW_COMPLETED_TOPIC));

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

    // ── 시나리오 2: CardReviewDue 라이브 컨테이너 검증 ──

    @Test
    @DisplayName("실제 Kafka 컨테이너에서 CardReviewDue Avro 이벤트가 발행되고 소비된다")
    void cardReviewDue_라이브컨테이너에서_Avro필드가_검증된다() {
        String userId   = UUID.randomUUID().toString();
        String tenantId = UUID.randomUUID().toString();
        int dueCount    = 5;
        String dueDate  = "2026-06-09";

        reviewDueEventPublisher.publish(userId, tenantId, dueCount, dueDate);

        ConsumerRecords<String, CardReviewDue> records = pollUntilNotEmpty(
                createAvroConsumer("rd-live-" + userId, REVIEW_DUE_TOPIC));

        assertThat(records.count()).isGreaterThan(0);
        CardReviewDue event = records.iterator().next().value();
        assertThat(event.getUserId().toString()).isEqualTo(userId);
        assertThat(event.getTenantId().toString()).isEqualTo(tenantId);
        assertThat(event.getDueCardCount()).isEqualTo(dueCount);
        assertThat(event.getDueDate().toString()).isEqualTo(dueDate);
        assertThat(event.getEventId().toString()).isNotBlank();
        assertThat(event.getOccurredAt()).isNotNull();
    }

    // ── 헬퍼 ──

    /**
     * 최대 15초간 반복 poll — 컨테이너 네트워크 지연 대응
     */
    @SuppressWarnings("unchecked")
    private <V> ConsumerRecords<String, V> pollUntilNotEmpty(Consumer<String, V> consumer) {
        ConsumerRecords<String, V> records = ConsumerRecords.empty();
        long deadline = System.currentTimeMillis() + 15_000;
        while (records.isEmpty() && System.currentTimeMillis() < deadline) {
            records = consumer.poll(Duration.ofSeconds(2));
        }
        consumer.close();
        return records;
    }

    @SuppressWarnings("unchecked")
    private <V> Consumer<String, V> createAvroConsumer(String groupId, String topic) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "10");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, MOCK_REGISTRY);
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
        DefaultKafkaConsumerFactory<String, Object> factory = new DefaultKafkaConsumerFactory<>(props);
        Consumer<String, V> consumer = (Consumer<String, V>) factory.createConsumer();
        consumer.subscribe(List.of(topic));
        return consumer;
    }
}
