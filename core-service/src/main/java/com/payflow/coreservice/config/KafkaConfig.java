package com.payflow.coreservice.config;

import com.payflow.commons.dto.alert.ManualReviewDecision;
import com.payflow.commons.dto.alert.PaymentAlertEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "core-group");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                "org.springframework.kafka.support.serializer.ErrorHandlingDeserializer");
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                "org.springframework.kafka.support.serializer.ErrorHandlingDeserializer");
        config.put("spring.json.trusted.packages", "com.payflow.commons.dto");
        config.put("spring.json.use.type.headers", "false");
        config.put("spring.deserializer.key.delegate.class",
                "org.apache.kafka.common.serialization.StringDeserializer");
        config.put("spring.deserializer.value.delegate.class",
                "org.springframework.kafka.support.serializer.JsonDeserializer");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PaymentAlertEvent> paymentAlertListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            KafkaTemplate<String, Object> kafkaTemplate){

        ConcurrentKafkaListenerContainerFactory<String, PaymentAlertEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        Map<String, Object> props = new HashMap<>(consumerFactory.getConfigurationProperties());
        props.put("spring.json.value.default.type" , PaymentAlertEvent.class.getName());

        DefaultKafkaConsumerFactory<String, PaymentAlertEvent> specificConsumerFactory =
                new DefaultKafkaConsumerFactory<>(props);

        factory.setConsumerFactory(specificConsumerFactory);
        factory.setCommonErrorHandler(getDefaultErrorHandler(kafkaTemplate));

        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String , ManualReviewDecision> manualDecisionListenerContainerFactory(
           ConsumerFactory<String, Object> consumerFactory,
           KafkaTemplate<String, Object> kafkaTemplate){

        ConcurrentKafkaListenerContainerFactory<String, ManualReviewDecision> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        Map<String, Object> props = new HashMap<>(consumerFactory.getConfigurationProperties());
        props.put("spring.json.value.default.type", ManualReviewDecision.class.getName());

        DefaultKafkaConsumerFactory<String, ManualReviewDecision> specificConsumerFactory =
                new DefaultKafkaConsumerFactory<>(props);

        factory.setConsumerFactory(specificConsumerFactory);
        factory.setCommonErrorHandler(getDefaultErrorHandler(kafkaTemplate));

        return factory;
    }

    @Bean
    public ConsumerFactory<String, String> dlqConsumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "dlq-group");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> dlqListenerContainerFactory(
            ConsumerFactory<String, String> dlqConsumerFactory){

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(dlqConsumerFactory);

        return factory;
    }

    private static DefaultErrorHandler getDefaultErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) ->{
                    String originalTopic = record.topic();
                    String dlqTopic = originalTopic + ".dlq";
                    return new TopicPartition(dlqTopic, record.partition());
                });

        FixedBackOff backOff = new FixedBackOff(1000, 3);
        return new DefaultErrorHandler(recoverer, backOff);
    }
}
