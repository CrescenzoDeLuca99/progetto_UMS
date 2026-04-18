package com.intesi.usermanagement.application.service;

import com.intesi.usermanagement.domain.enums.RoleName;
import com.intesi.usermanagement.domain.enums.UserStatus;
import com.intesi.usermanagement.domain.model.Role;
import com.intesi.usermanagement.domain.model.User;
import com.intesi.usermanagement.dto.request.CreateUserRequest;
import com.intesi.usermanagement.dto.response.UserResponse;
import com.intesi.usermanagement.exception.RoleNotFoundException;
import com.intesi.usermanagement.exception.UserAlreadyExistsException;
import com.intesi.usermanagement.infrastructure.dao.RoleDao;
import com.intesi.usermanagement.infrastructure.dao.UserDao;
import com.intesi.usermanagement.infrastructure.messaging.UserEventPublisher;
import com.intesi.usermanagement.infrastructure.messaging.UserEventType;
import com.intesi.usermanagement.mapper.UserMapper;
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
    private UserDao userDao;

    @Mock
    private RoleDao roleDao;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserEventPublisher eventPublisher;

    @InjectMocks
    private UserService userService;

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
        UserResponse expectedResponse = UserResponse.builder()
                .id(1L)
                .username("mrossi")
                .email("mario.rossi@example.com")
                .build();

        when(userDao.existsByUsername("mrossi")).thenReturn(false);
        when(userDao.existsByEmail("mario.rossi@example.com")).thenReturn(false);
        when(userDao.existsByCodiceFiscale("rssmra85m01h501z")).thenReturn(false);
        when(roleDao.findByName(RoleName.DEVELOPER)).thenReturn(Optional.of(developerRole));
        when(userDao.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toResponse(savedUser)).thenReturn(expectedResponse);

        UserResponse result = userService.createUser(validRequest);

        assertThat(result).isEqualTo(expectedResponse);
        verify(userDao).save(any(User.class));
    }

    @Test
    void shouldNormalizeCodiceFiscaleToUppercase() {
        Role developerRole = new Role(RoleName.DEVELOPER);
        User savedUser = User.builder().id(1L).build();

        when(userDao.existsByUsername(any())).thenReturn(false);
        when(userDao.existsByEmail(any())).thenReturn(false);
        when(userDao.existsByCodiceFiscale(any())).thenReturn(false);
        when(roleDao.findByName(any())).thenReturn(Optional.of(developerRole));
        when(userDao.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toResponse(any())).thenReturn(new UserResponse());

        userService.createUser(validRequest); // codiceFiscale è lowercase nel setUp

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userDao).save(captor.capture());
        assertThat(captor.getValue().getCodiceFiscale()).isEqualTo("RSSMRA85M01H501Z");
    }

    // -------------------------------------------------------------------------
    // Unicità: username, email, codiceFiscale
    // -------------------------------------------------------------------------

    @Test
    void shouldThrowWhenUsernameAlreadyExists() {
        when(userDao.existsByUsername("mrossi")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(validRequest))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("username");

        verify(userDao, never()).save(any());
    }

    @Test
    void shouldThrowWhenEmailAlreadyExists() {
        when(userDao.existsByUsername(any())).thenReturn(false);
        when(userDao.existsByEmail("mario.rossi@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(validRequest))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("email");

        verify(userDao, never()).save(any());
    }

    @Test
    void shouldThrowWhenCodiceFiscaleAlreadyExists() {
        when(userDao.existsByUsername(any())).thenReturn(false);
        when(userDao.existsByEmail(any())).thenReturn(false);
        when(userDao.existsByCodiceFiscale("rssmra85m01h501z")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(validRequest))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("codiceFiscale");

        verify(userDao, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // Pubblicazione eventi
    // -------------------------------------------------------------------------

    @Test
    void shouldPublishCreatedEventOnSuccess() {
        Role developerRole = new Role(RoleName.DEVELOPER);
        User savedUser = User.builder().id(1L).username("mrossi").build();

        when(userDao.existsByUsername(any())).thenReturn(false);
        when(userDao.existsByEmail(any())).thenReturn(false);
        when(userDao.existsByCodiceFiscale(any())).thenReturn(false);
        when(roleDao.findByName(any())).thenReturn(Optional.of(developerRole));
        when(userDao.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toResponse(any())).thenReturn(new UserResponse());

        userService.createUser(validRequest);

        verify(eventPublisher).publish(UserEventType.CREATED, savedUser);
    }

    @Test
    void shouldNotPublishEventOnFailure() {
        when(userDao.existsByUsername("mrossi")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(validRequest))
                .isInstanceOf(UserAlreadyExistsException.class);

        verify(eventPublisher, never()).publish(any(), any());
    }

    // -------------------------------------------------------------------------
    // Ruolo non trovato in DB
    // -------------------------------------------------------------------------

    @Test
    void shouldThrowWhenRoleNotFoundInDatabase() {
        when(userDao.existsByUsername(any())).thenReturn(false);
        when(userDao.existsByEmail(any())).thenReturn(false);
        when(userDao.existsByCodiceFiscale(any())).thenReturn(false);
        when(roleDao.findByName(RoleName.DEVELOPER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.createUser(validRequest))
                .isInstanceOf(RoleNotFoundException.class)
                .hasMessageContaining("DEVELOPER");

        verify(userDao, never()).save(any());
    }
}
