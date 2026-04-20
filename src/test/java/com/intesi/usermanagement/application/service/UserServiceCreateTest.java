package com.intesi.usermanagement.application.service;

import com.intesi.usermanagement.application.port.out.RolePersistencePort;
import com.intesi.usermanagement.application.port.out.UserEventPort;
import com.intesi.usermanagement.application.port.out.UserPersistencePort;
import com.intesi.usermanagement.domain.enums.RoleName;
import com.intesi.usermanagement.domain.enums.UserEventType;
import com.intesi.usermanagement.domain.enums.UserStatus;
import com.intesi.usermanagement.domain.model.Role;
import com.intesi.usermanagement.domain.model.User;
import com.intesi.usermanagement.dto.request.CreateUserRequest;
import com.intesi.usermanagement.exception.RoleNotFoundException;
import com.intesi.usermanagement.exception.UserAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceCreateTest {

    @Mock
    private UserPersistencePort userPersistence;

    @Mock
    private RolePersistencePort rolePersistence;

    @Mock
    private UserEventPort eventPort;

    @InjectMocks
    private UserCommandService userService;

    private CreateUserRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = CreateUserRequest.builder()
                .username("mrossi")
                .email("mario.rossi@example.com")
                .codiceFiscale("rssmra85m01h501z")
                .nome("Mario")
                .cognome("Rossi")
                .roles(Set.of(RoleName.DEVELOPER))
                .build();
    }

    // -------------------------------------------------------------------------
    // Happy path
    // -------------------------------------------------------------------------

    @Test
    void shouldCreateUserAndReturnResponse() {
        Role developerRole = new Role(RoleName.DEVELOPER);
        User savedUser = User.builder()
                .id(1L)
                .username("mrossi")
                .email("mario.rossi@example.com")
                .codiceFiscale("RSSMRA85M01H501Z")
                .nome("Mario")
                .cognome("Rossi")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(developerRole))
                .build();

        when(userPersistence.existsByUsername("mrossi")).thenReturn(false);
        when(userPersistence.existsByEmail("mario.rossi@example.com")).thenReturn(false);
        when(userPersistence.existsByCodiceFiscale("rssmra85m01h501z")).thenReturn(false);
        when(rolePersistence.findByName(RoleName.DEVELOPER)).thenReturn(Optional.of(developerRole));
        when(userPersistence.save(any(User.class))).thenReturn(savedUser);

        User result = userService.createUser(validRequest);

        assertThat(result).isEqualTo(savedUser);
        verify(userPersistence).save(any(User.class));
    }

    @Test
    void shouldNormalizeCodiceFiscaleToUppercase() {
        Role developerRole = new Role(RoleName.DEVELOPER);
        User savedUser = User.builder().id(1L).build();

        when(userPersistence.existsByUsername(any())).thenReturn(false);
        when(userPersistence.existsByEmail(any())).thenReturn(false);
        when(userPersistence.existsByCodiceFiscale(any())).thenReturn(false);
        when(rolePersistence.findByName(any())).thenReturn(Optional.of(developerRole));
        when(userPersistence.save(any(User.class))).thenReturn(savedUser);

        userService.createUser(validRequest); // codiceFiscale è lowercase nel setUp

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userPersistence).save(captor.capture());
        assertThat(captor.getValue().getCodiceFiscale()).isEqualTo("RSSMRA85M01H501Z");
    }

    // -------------------------------------------------------------------------
    // Unicità: username, email, codiceFiscale
    // -------------------------------------------------------------------------

    @Test
    void shouldThrowWhenUsernameAlreadyExists() {
        when(userPersistence.existsByUsername("mrossi")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(validRequest))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("username");

        verify(userPersistence, never()).save(any());
    }

    @Test
    void shouldThrowWhenEmailAlreadyExists() {
        when(userPersistence.existsByUsername(any())).thenReturn(false);
        when(userPersistence.existsByEmail("mario.rossi@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(validRequest))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("email");

        verify(userPersistence, never()).save(any());
    }

    @Test
    void shouldThrowWhenCodiceFiscaleAlreadyExists() {
        when(userPersistence.existsByUsername(any())).thenReturn(false);
        when(userPersistence.existsByEmail(any())).thenReturn(false);
        when(userPersistence.existsByCodiceFiscale("rssmra85m01h501z")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(validRequest))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("codiceFiscale");

        verify(userPersistence, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // Pubblicazione eventi
    // -------------------------------------------------------------------------

    @Test
    void shouldPublishCreatedEventOnSuccess() {
        Role developerRole = new Role(RoleName.DEVELOPER);
        User savedUser = User.builder().id(1L).username("mrossi").build();

        when(userPersistence.existsByUsername(any())).thenReturn(false);
        when(userPersistence.existsByEmail(any())).thenReturn(false);
        when(userPersistence.existsByCodiceFiscale(any())).thenReturn(false);
        when(rolePersistence.findByName(any())).thenReturn(Optional.of(developerRole));
        when(userPersistence.save(any(User.class))).thenReturn(savedUser);

        userService.createUser(validRequest);

        verify(eventPort).publish(UserEventType.CREATED, savedUser);
    }

    @Test
    void shouldNotPublishEventOnFailure() {
        when(userPersistence.existsByUsername("mrossi")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(validRequest))
                .isInstanceOf(UserAlreadyExistsException.class);

        verify(eventPort, never()).publish(any(), any());
    }

    // -------------------------------------------------------------------------
    // Ruolo non trovato in DB
    // -------------------------------------------------------------------------

    @Test
    void shouldThrowWhenRoleNotFoundInDatabase() {
        when(userPersistence.existsByUsername(any())).thenReturn(false);
        when(userPersistence.existsByEmail(any())).thenReturn(false);
        when(userPersistence.existsByCodiceFiscale(any())).thenReturn(false);
        when(rolePersistence.findByName(RoleName.DEVELOPER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.createUser(validRequest))
                .isInstanceOf(RoleNotFoundException.class)
                .hasMessageContaining("DEVELOPER");

        verify(userPersistence, never()).save(any());
    }
}
