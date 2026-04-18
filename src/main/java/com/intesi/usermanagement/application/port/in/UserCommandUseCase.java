package com.intesi.usermanagement.application.port.in;

import com.intesi.usermanagement.dto.request.CreateUserRequest;
import com.intesi.usermanagement.dto.request.UpdateUserRequest;
import com.intesi.usermanagement.dto.response.UserResponse;

public interface UserCommandUseCase {

    UserResponse createUser(CreateUserRequest request);

    UserResponse updateUser(Long id, UpdateUserRequest request);

    void disableUser(Long id);

    void enableUser(Long id);

    void deleteUser(Long id);
}
