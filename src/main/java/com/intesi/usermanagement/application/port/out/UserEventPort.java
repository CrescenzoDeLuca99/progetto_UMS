package com.intesi.usermanagement.application.port.out;

import com.intesi.usermanagement.domain.enums.UserEventType;
import com.intesi.usermanagement.domain.model.User;

public interface UserEventPort {

    void publish(UserEventType eventType, User user);
}
