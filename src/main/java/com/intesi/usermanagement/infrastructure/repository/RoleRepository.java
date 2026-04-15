package com.intesi.usermanagement.infrastructure.repository;

import com.intesi.usermanagement.domain.enums.RoleName;
import com.intesi.usermanagement.domain.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Short> {

    Optional<Role> findByName(RoleName name);
}
