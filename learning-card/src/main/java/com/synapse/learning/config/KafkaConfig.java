package com.synapse.learning.config;

import com.synapse.event.learning.CardReviewDue;
import com.synapse.learning.ReviewCompleted;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
@ConditionalOnProperty(prefix = "synapse.kafka", name = "enabled", havingValue = "true")
public class KafkaConfig {

    @Value("${spring.kafka.security.protocol:PLAINTEXT}")
    private String securityProtocol;

    @Bean
    public ProducerFactory<String, ReviewCompleted> reviewCompletedProducerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${spring.kafka.producer.properties.schema.registry.url}") String schemaRegistryUrl,
            @Value("${spring.kafka.producer.acks:all}") String acks,
            @Value("${spring.kafka.producer.retries:3}") int retries,
            @Value("${spring.kafka.producer.properties.retry.backoff.ms:1000}") int retryBackoffMs) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        props.put(ProducerConfig.ACKS_CONFIG, acks);
        props.put(ProducerConfig.RETRIES_CONFIG, retries);
        props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, retryBackoffMs);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        if (!"PLAINTEXT".equalsIgnoreCase(securityProtocol)) {
            props.put("security.protocol", securityProtocol);
        }
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, ReviewCompleted> reviewCompletedKafkaTemplate(
            ProducerFactory<String, ReviewCompleted> reviewCompletedProducerFactory) {
        return new KafkaTemplate<>(reviewCompletedProducerFactory);
    }

    @Bean
    public ProducerFactory<String, CardReviewDue> reviewDueProducerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${spring.kafka.producer.properties.schema.registry.url}") String schemaRegistryUrl,
            @Value("${spring.kafka.producer.acks:all}") String acks,
            @Value("${spring.kafka.producer.retries:3}") int retries,
            @Value("${spring.kafka.producer.properties.retry.backoff.ms:1000}") int retryBackoffMs) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        props.put(ProducerConfig.ACKS_CONFIG, acks);
        props.put(ProducerConfig.RETRIES_CONFIG, retries);
        props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, retryBackoffMs);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        if (!"PLAINTEXT".equalsIgnoreCase(securityProtocol)) {
            props.put("security.protocol", securityProtocol);
        }
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, CardReviewDue> reviewDueKafkaTemplate(
            ProducerFactory<String, CardReviewDue> reviewDueProducerFactory) {
        return new KafkaTemplate<>(reviewDueProducerFactory);
    }

    @Bean
    public KafkaTemplate<String, String> dlqKafkaTemplate(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        if (!"PLAINTEXT".equalsIgnoreCase(securityProtocol)) {
            props.put("security.protocol", securityProtocol);
        }
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }

    @Bean
    public ConsumerFactory<String, Object> consumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${spring.kafka.consumer.properties.schema.registry.url}") String schemaRegistryUrl) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        props.put("specific.avro.reader", true);
        if (!"PLAINTEXT".equalsIgnoreCase(securityProtocol)) {
            props.put("security.protocol", securityProtocol);
        }
        return new DefaultKafkaConsumerFactory<>(props);
    }
}
