package com.intesi.usermanagement.application.service;

import com.intesi.usermanagement.application.port.in.UserQueryUseCase;
import com.intesi.usermanagement.application.port.out.UserPersistencePort;
import com.intesi.usermanagement.domain.enums.UserStatus;
import com.intesi.usermanagement.domain.model.User;
import com.intesi.usermanagement.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserQueryService implements UserQueryUseCase {

    private final UserPersistencePort userPersistence;

    @Override
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        log.debug("Recupero utente id={}", id);
        return userPersistence.findByIdAndStatusNot(id, UserStatus.DELETED)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> listUsers(Pageable pageable) {
        log.debug("Lista utenti: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        return userPersistence.findAllExcludingStatus(UserStatus.DELETED, pageable);
    }
}
