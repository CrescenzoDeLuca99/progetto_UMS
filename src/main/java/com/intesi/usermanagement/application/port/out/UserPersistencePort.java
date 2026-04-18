package com.intesi.usermanagement.application.port.out;

import com.intesi.usermanagement.domain.enums.UserStatus;
import com.intesi.usermanagement.domain.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface UserPersistencePort {

    User save(User user);

    Optional<User> findByIdAndStatusNot(Long id, UserStatus status);

    Page<User> findAllExcludingStatus(UserStatus excluded, Pageable pageable);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByCodiceFiscale(String codiceFiscale);

    boolean existsByUsernameAndIdNot(String username, Long id);
}
