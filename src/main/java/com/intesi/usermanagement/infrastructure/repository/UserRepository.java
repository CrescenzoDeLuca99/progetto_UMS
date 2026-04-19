package com.intesi.usermanagement.infrastructure.repository;

import com.intesi.usermanagement.domain.enums.UserStatus;
import com.intesi.usermanagement.domain.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByCodiceFiscale(String codiceFiscale);

    Optional<User> findByIdAndStatusNot(Long id, UserStatus status);

    boolean existsByUsernameAndIdNot(String username, Long id);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.id = :id")
    Optional<User> findByIdWithRoles(@Param("id") Long id);

    @Query("""
            SELECT DISTINCT u FROM User u
            JOIN FETCH u.roles r
            WHERE u.status != :excluded
            """)
    Page<User> findAllExcludingStatus(@Param("excluded") UserStatus excluded, Pageable pageable);
}
