package com.intesi.usermanagement.infrastructure.dao.impl;

import com.intesi.usermanagement.domain.enums.RoleName;
import com.intesi.usermanagement.domain.model.Role;
import com.intesi.usermanagement.infrastructure.dao.RoleDao;
import com.intesi.usermanagement.infrastructure.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RoleDaoImpl implements RoleDao {

    private final RoleRepository repository;

    @Autowired
    @Lazy
    private RoleDaoImpl self;

    @Override
    public Optional<Role> findByName(RoleName name) {
        return Optional.ofNullable(self.loadRole(name));
    }

    @Cacheable(value = "roles", key = "#name.name()")
    public Role loadRole(RoleName name) {
        return repository.findByName(name).orElse(null);
    }
}
