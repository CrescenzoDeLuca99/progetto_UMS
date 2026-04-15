package com.intesi.usermanagement.application.service;

import com.intesi.usermanagement.domain.enums.RoleName;
import com.intesi.usermanagement.domain.enums.UserStatus;
import com.intesi.usermanagement.domain.model.Role;
import com.intesi.usermanagement.domain.model.User;
import com.intesi.usermanagement.dto.request.CreateUserRequest;
import com.intesi.usermanagement.dto.response.UserResponse;
import com.intesi.usermanagement.exception.RoleNotFoundException;
import com.intesi.usermanagement.exception.UserAlreadyExistsException;
import com.intesi.usermanagement.infrastructure.repository.RoleRepository;
import com.intesi.usermanagement.infrastructure.repository.UserRepository;
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
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserMapper userMapper;

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

        when(userRepository.existsByUsername("mrossi")).thenReturn(false);
        when(userRepository.existsByEmail("mario.rossi@example.com")).thenReturn(false);
        when(userRepository.existsByCodiceFiscale("rssmra85m01h501z")).thenReturn(false);
        when(roleRepository.findByName(RoleName.DEVELOPER)).thenReturn(Optional.of(developerRole));
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toResponse(savedUser)).thenReturn(expectedResponse);

        UserResponse result = userService.createUser(validRequest);

        assertThat(result).isEqualTo(expectedResponse);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldNormalizeCodiceFiscaleToUppercase() {
        Role developerRole = new Role(RoleName.DEVELOPER);
        User savedUser = User.builder().id(1L).build();

        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByCodiceFiscale(any())).thenReturn(false);
        when(roleRepository.findByName(any())).thenReturn(Optional.of(developerRole));
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toResponse(any())).thenReturn(new UserResponse());

        userService.createUser(validRequest); // codiceFiscale è lowercase nel setUp

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getCodiceFiscale()).isEqualTo("RSSMRA85M01H501Z");
    }

    // -------------------------------------------------------------------------
    // Unicità: username, email, codiceFiscale
    // -------------------------------------------------------------------------

    @Test
    void shouldThrowWhenUsernameAlreadyExists() {
        when(userRepository.existsByUsername("mrossi")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(validRequest))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("username");

        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenEmailAlreadyExists() {
        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(userRepository.existsByEmail("mario.rossi@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(validRequest))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("email");

        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenCodiceFiscaleAlreadyExists() {
        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByCodiceFiscale("rssmra85m01h501z")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(validRequest))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("codiceFiscale");

        verify(userRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // Ruolo non trovato in DB
    // -------------------------------------------------------------------------

    @Test
    void shouldThrowWhenRoleNotFoundInDatabase() {
        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByCodiceFiscale(any())).thenReturn(false);
        when(roleRepository.findByName(RoleName.DEVELOPER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.createUser(validRequest))
                .isInstanceOf(RoleNotFoundException.class)
                .hasMessageContaining("DEVELOPER");

        verify(userRepository, never()).save(any());
    }
}
