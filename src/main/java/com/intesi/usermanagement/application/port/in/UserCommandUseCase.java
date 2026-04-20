package com.intesi.usermanagement.application.port.in;

import com.intesi.usermanagement.domain.model.User;
import com.intesi.usermanagement.dto.request.CreateUserRequest;
import com.intesi.usermanagement.dto.request.UpdateUserRequest;

public interface UserCommandUseCase {

    User createUser(CreateUserRequest request);

    User updateUser(Long id, UpdateUserRequest request);

    void disableUser(Long id);

    void enableUser(Long id);

    void deleteUser(Long id);
}
