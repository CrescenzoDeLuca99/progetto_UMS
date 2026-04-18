package com.intesi.usermanagement.infrastructure.messaging;

import com.intesi.usermanagement.domain.enums.UserEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEvent {

    private UserEventType eventType;
    private Long userId;
    private String username;
    private LocalDateTime timestamp;
}
