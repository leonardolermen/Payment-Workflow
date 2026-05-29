package com.payflow.coreservice.config;

import com.payflow.commons.dto.alert.ManualReviewDecision;
import com.payflow.commons.dto.alert.PaymentAlertEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // ─── Shared error handler ────────────────────────────────────────────────────

    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(
            @Lazy KafkaTemplate<String, Object> kafkaTemplate) {
        return new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + ".dlq", record.partition())
        );
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(DeadLetterPublishingRecoverer recoverer) {
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
    }

    // ─── PaymentAlertEvent consumer ──────────────────────────────────────────────

    @Bean
    public ConsumerFactory<String, PaymentAlertEvent> paymentAlertConsumerFactory() {
        JsonDeserializer<PaymentAlertEvent> deserializer = new JsonDeserializer<>(PaymentAlertEvent.class);
        deserializer.setRemoveTypeHeaders(true);
        deserializer.addTrustedPackages("com.payflow.commons.dto");

        return new DefaultKafkaConsumerFactory<>(
                baseConsumerProps("alert-group"),
                new StringDeserializer(),
                new ErrorHandlingDeserializer<>(deserializer)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PaymentAlertEvent> paymentAlertListenerContainerFactory(
            DefaultErrorHandler errorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, PaymentAlertEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(paymentAlertConsumerFactory());
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }

    // ─── ManualReviewDecision consumer ───────────────────────────────────────────

    @Bean
    public ConsumerFactory<String, ManualReviewDecision> manualDecisionConsumerFactory() {
        JsonDeserializer<ManualReviewDecision> deserializer = new JsonDeserializer<>(ManualReviewDecision.class);
        deserializer.setRemoveTypeHeaders(true);
        deserializer.addTrustedPackages("com.payflow.commons.dto");

        return new DefaultKafkaConsumerFactory<>(
                baseConsumerProps("manual-decision-group"),
                new StringDeserializer(),
                new ErrorHandlingDeserializer<>(deserializer)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ManualReviewDecision> manualDecisionListenerContainerFactory(
            DefaultErrorHandler errorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, ManualReviewDecision> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(manualDecisionConsumerFactory());
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }

    // ─── DLQ consumer (raw String) ───────────────────────────────────────────────

    @Bean
    public ConsumerFactory<String, String> dlqConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(
                baseConsumerProps("dlq-group"),
                new StringDeserializer(),
                new StringDeserializer()
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> dlqListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(dlqConsumerFactory());
        return factory;
    }

    // ─── Base consumer props ─────────────────────────────────────────────────────

    private Map<String, Object> baseConsumerProps(String groupId) {
        return Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG, groupId,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class
        );
    }
}
