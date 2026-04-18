package com.intesi.usermanagement.infrastructure.dao.impl;

import com.intesi.usermanagement.application.port.out.UserPersistencePort;
import com.intesi.usermanagement.domain.enums.UserStatus;
import com.intesi.usermanagement.domain.model.User;
import com.intesi.usermanagement.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserDaoImpl implements UserPersistencePort {

    private final UserRepository repository;

    /*
     * Self-injection via proxy: necessario per far sì che @Cacheable su loadById
     * venga intercettato da Spring AOP (le chiamate interne bypassano il proxy).
     */
    @Autowired
    @Lazy
    private UserDaoImpl self;

    @Override
    @CachePut(value = "users", key = "#result.id")
    public User save(User user) {
        return repository.save(user);
    }

    @Override
    public Optional<User> findByIdAndStatusNot(Long id, UserStatus status) {
        return Optional.ofNullable(self.loadById(id))
                .filter(u -> u.getStatus() != status);
    }

    // restituisce null anziché Optional: @Cacheable non gestisce bene Optional come valore cacheable
    @Cacheable(value = "users", key = "#id")
    public User loadById(Long id) {
        log.debug("Cache miss utente id={}, fetch da DB", id);
        return repository.findByIdWithRoles(id).orElse(null);
    }

    @Override
    public Page<User> findAllExcludingStatus(UserStatus excluded, Pageable pageable) {
        return repository.findAllExcludingStatus(excluded, pageable);
    }

    @Override
    public boolean existsByUsername(String username) {
        return repository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    @Override
    public boolean existsByCodiceFiscale(String codiceFiscale) {
        return repository.existsByCodiceFiscale(codiceFiscale);
    }

    @Override
    public boolean existsByUsernameAndIdNot(String username, Long id) {
        return repository.existsByUsernameAndIdNot(username, id);
    }
}
