package com.intesi.usermanagement.application.service;

import com.intesi.usermanagement.application.port.in.UserCommandUseCase;
import com.intesi.usermanagement.application.port.out.RolePersistencePort;
import com.intesi.usermanagement.application.port.out.UserEventPort;
import com.intesi.usermanagement.application.port.out.UserPersistencePort;
import com.intesi.usermanagement.domain.enums.UserEventType;
import com.intesi.usermanagement.domain.enums.UserStatus;
import com.intesi.usermanagement.domain.model.Role;
import com.intesi.usermanagement.domain.model.User;
import com.intesi.usermanagement.dto.request.CreateUserRequest;
import com.intesi.usermanagement.dto.request.UpdateUserRequest;
import com.intesi.usermanagement.exception.RoleNotFoundException;
import com.intesi.usermanagement.exception.UserAlreadyExistsException;
import com.intesi.usermanagement.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCommandService implements UserCommandUseCase {

    private final UserPersistencePort userPersistence;
    private final RolePersistencePort rolePersistence;
    private final UserEventPort eventPort;

    @Override
    @Transactional
    public User createUser(CreateUserRequest request) {
        log.info("Creazione utente: username={}, email={}", request.getUsername(), request.getEmail());
        if (userPersistence.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("username", request.getUsername());
        }
        if (userPersistence.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("email", request.getEmail());
        }
        if (userPersistence.existsByCodiceFiscale(request.getCodiceFiscale())) {
            throw new UserAlreadyExistsException("codiceFiscale", request.getCodiceFiscale());
        }

        Set<Role> roles = resolveRoles(request);

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .codiceFiscale(request.getCodiceFiscale().toUpperCase()) // normalizzazione: CF salvato sempre maiuscolo
                .nome(request.getNome())
                .cognome(request.getCognome())
                .roles(roles)
                .build();

        User saved = userPersistence.save(user);
        log.info("Utente creato: id={}, username={}", saved.getId(), saved.getUsername());
        eventPort.publish(UserEventType.CREATED, saved);
        return saved;
    }

    @Override
    @Transactional
    public User updateUser(Long id, UpdateUserRequest request) {
        log.info("Aggiornamento utente id={}", id);
        User user = userPersistence.findByIdAndStatusNot(id, UserStatus.DELETED)
                .orElseThrow(() -> new UserNotFoundException(id));

        // controlla unicità solo se il nome utente è effettivamente cambiato, evitando falsi conflitti su se stesso
        if (!user.getUsername().equals(request.getUsername()) &&
                userPersistence.existsByUsernameAndIdNot(request.getUsername(), id)) {
            throw new UserAlreadyExistsException("username", request.getUsername());
        }

        Set<Role> roles = request.getRoles().stream()
                .map(roleName -> rolePersistence.findByName(roleName)
                        .orElseThrow(() -> new RoleNotFoundException(roleName)))
                .collect(Collectors.toSet());

        user.setUsername(request.getUsername());
        user.setNome(request.getNome());
        user.setCognome(request.getCognome());
        user.setRoles(roles);

        User saved = userPersistence.save(user);
        log.info("Utente aggiornato: id={}", saved.getId());
        eventPort.publish(UserEventType.UPDATED, saved);
        return saved;
    }

    @Override
    @Transactional
    public void disableUser(Long id) {
        User user = userPersistence.findByIdAndStatusNot(id, UserStatus.DELETED)
                .orElseThrow(() -> new UserNotFoundException(id));
        user.setStatus(UserStatus.DISABLED);
        userPersistence.save(user);
        log.info("Utente disabilitato: id={}", id);
        eventPort.publish(UserEventType.DISABLED, user);
    }

    @Override
    @Transactional
    public void enableUser(Long id) {
        User user = userPersistence.findByIdAndStatusNot(id, UserStatus.DELETED)
                .orElseThrow(() -> new UserNotFoundException(id));
        user.setStatus(UserStatus.ACTIVE);
        userPersistence.save(user);
        log.info("Utente abilitato: id={}", id);
        eventPort.publish(UserEventType.ENABLED, user);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        // soft-delete: il record rimane in DB per audit, ma viene escluso da tutte le query operative
        User user = userPersistence.findByIdAndStatusNot(id, UserStatus.DELETED)
                .orElseThrow(() -> new UserNotFoundException(id));
        user.setStatus(UserStatus.DELETED);
        userPersistence.save(user);
        log.info("Utente eliminato (soft-delete): id={}", id);
        eventPort.publish(UserEventType.DELETED, user);
    }

    private Set<Role> resolveRoles(CreateUserRequest request) {
        return request.getRoles().stream()
                .map(roleName -> rolePersistence.findByName(roleName)
                        .orElseThrow(() -> new RoleNotFoundException(roleName)))
                .collect(Collectors.toSet());
    }
}
