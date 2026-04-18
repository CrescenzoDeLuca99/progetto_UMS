package com.intesi.usermanagement.infrastructure.dao;

import com.intesi.usermanagement.domain.enums.RoleName;
import com.intesi.usermanagement.domain.model.Role;

import java.util.Optional;

public interface RoleDao {

    Optional<Role> findByName(RoleName name);
}
