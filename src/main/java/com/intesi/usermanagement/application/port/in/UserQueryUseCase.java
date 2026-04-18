package com.intesi.usermanagement.application.port.in;

import com.intesi.usermanagement.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserQueryUseCase {

    UserResponse getUserById(Long id);

    Page<UserResponse> listUsers(Pageable pageable);
}
