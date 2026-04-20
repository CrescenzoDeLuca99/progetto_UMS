package com.intesi.usermanagement.application.port.in;

import com.intesi.usermanagement.domain.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserQueryUseCase {

    User getUserById(Long id);

    Page<User> listUsers(Pageable pageable);
}
