package com.intesi.usermanagement.infrastructure.messaging;

import com.intesi.usermanagement.application.port.out.UserEventPort;
import com.intesi.usermanagement.domain.enums.UserEventType;
import com.intesi.usermanagement.domain.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventPublisher implements UserEventPort {

    @Value("${app.kafka.topics.user-events}")
    private String topic;

    private final KafkaTemplate<String, UserEvent> kafkaTemplate;

    @Override
    public void publish(UserEventType eventType, User user) {
        UserEvent event = UserEvent.builder()
                .eventType(eventType)
                .userId(user.getId())
                .username(user.getUsername())
                .timestamp(LocalDateTime.now())
                .build();
        log.debug("Pubblicazione evento Kafka: type={}, userId={}", eventType, user.getId());
        // fire-and-forget asincrono: il callback non blocca il thread chiamante
        kafkaTemplate.send(topic, String.valueOf(user.getId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Errore pubblicazione evento {}: userId={}", eventType, user.getId(), ex);
                    } else {
                        log.debug("Evento {} pubblicato: offset={}", eventType,
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
