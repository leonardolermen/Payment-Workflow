package com.payflow.coreservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicsConfig {

    @Bean
    public NewTopic paymentRequestedTopic(){
        return TopicBuilder.name("payflow.payment.requested")
                .partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic fraudCompletedTopic(){
        return TopicBuilder.name("payflow.fraud.completed")
                .partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic paymentAlertsTopic(){
        return TopicBuilder.name("payflow.payment.alerts")
                .partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic transactionCompletedTopic(){
        return TopicBuilder.name("payflow.transaction.completed")
                .partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic reviewCompletedTopic(){
        return TopicBuilder.name("payflow.review.completed")
                .partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic paymentAlertsDLQ(){
        return TopicBuilder.name("payflow.payment.alerts.dlt")
                .partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic reviewCompletedDLQ(){
        return TopicBuilder.name("payflow.review.completed.dlt")
                .partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic manualDecisionTopic(){
        return TopicBuilder.name("payflow.manual.decision")
                .partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic manualDecisionDLQ(){
        return TopicBuilder.name("payflow.manual.decision.dlt")
                .partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic userCreatedTopic() {
        return TopicBuilder.name("payflow.user.created")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userKycResultTopic() {
        return TopicBuilder.name("payflow.user.kyc.result")
                .partitions(3)
                .replicas(1)
                .build();
    }

}
