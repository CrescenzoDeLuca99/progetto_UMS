package com.intesi.usermanagement.infrastructure.dao.impl;

import com.intesi.usermanagement.application.port.out.RolePersistencePort;
import com.intesi.usermanagement.domain.enums.RoleName;
import com.intesi.usermanagement.domain.model.Role;
import com.intesi.usermanagement.infrastructure.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoleDaoImpl implements RolePersistencePort {

    private final RoleRepository repository;

    // self-injection via proxy: necessario affinché @Cacheable su loadRole venga intercettato da Spring AOP
    @Autowired
    @Lazy
    private RoleDaoImpl self;

    @Override
    public Optional<Role> findByName(RoleName name) {
        return Optional.ofNullable(self.loadRole(name));
    }

    // restituisce null anziché Optional: @Cacheable non gestisce bene Optional come valore cacheable
    @Cacheable(value = "roles", key = "#name.name()")
    public Role loadRole(RoleName name) {
        log.debug("Cache miss ruolo={}, fetch da DB", name);
        return repository.findByName(name).orElse(null);
    }
}
