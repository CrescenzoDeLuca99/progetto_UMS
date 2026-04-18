package com.intesi.usermanagement.infrastructure.messaging;

import com.intesi.usermanagement.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class UserEventPublisher {

    @Value("${app.kafka.topics.user-events}")
    private String topic;

    private final KafkaTemplate<String, UserEvent> kafkaTemplate;

    public void publish(UserEventType eventType, User user) {
        UserEvent event = UserEvent.builder()
                .eventType(eventType)
                .userId(user.getId())
                .username(user.getUsername())
                .timestamp(LocalDateTime.now())
                .build();
        kafkaTemplate.send(topic, String.valueOf(user.getId()), event);
    }
}
