package com.payflow.coreservice.producer;

import com.payflow.commons.dto.user.UserCreatedEvent;
import com.payflow.coreservice.model.User;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Builder
@Service
@RequiredArgsConstructor
@Slf4j
public class UserCreatedProducer {

    private static final String TOPIC = "payflow.user.created";

    private final KafkaTemplate<String, UserCreatedEvent> kafkaTemplate;

    public void publish(User user) {

        UserCreatedEvent event = UserCreatedEvent.builder()
                .userId(user.getUuid())
                .name(user.getName())
                .email(user.getEmail())
                .document(user.getDocument())
                .documentType(user.getDocumentType())
                .createdAt(user.getCreatedAt())
                .build();

        kafkaTemplate.send(
                TOPIC,
                user.getUuid().toString(),
                event
        );

        log.info(
                "Evento UserCreated enviado. userId={}",
                user.getUuid()
        );
    }
}
