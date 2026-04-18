package com.intesi.usermanagement.application.port.out;

import com.intesi.usermanagement.domain.enums.RoleName;
import com.intesi.usermanagement.domain.model.Role;

import java.util.Optional;

public interface RolePersistencePort {

    Optional<Role> findByName(RoleName name);
}
