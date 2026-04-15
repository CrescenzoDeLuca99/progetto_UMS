package com.intesi.usermanagement.application.service;

import com.intesi.usermanagement.domain.enums.RoleName;
import com.intesi.usermanagement.domain.enums.UserStatus;
import com.intesi.usermanagement.domain.model.Role;
import com.intesi.usermanagement.domain.model.User;
import com.intesi.usermanagement.dto.request.UpdateUserRequest;
import com.intesi.usermanagement.dto.response.UserResponse;
import com.intesi.usermanagement.exception.RoleNotFoundException;
import com.intesi.usermanagement.exception.UserAlreadyExistsException;
import com.intesi.usermanagement.exception.UserNotFoundException;
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
class UserServiceUpdateStatusTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private User existingUser;
    private UpdateUserRequest validRequest;

    @BeforeEach
    void setUp() {
        existingUser = User.builder()
                .id(1L)
                .username("mrossi")
                .email("mario.rossi@example.com")
                .codiceFiscale("RSSMRA85M01H501Z")
                .nome("Mario")
                .cognome("Rossi")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(new Role(RoleName.DEVELOPER)))
                .build();

        validRequest = UpdateUserRequest.builder()
                .username("mrossi")
                .nome("Mario")
                .cognome("Rossi")
                .roles(Set.of(RoleName.DEVELOPER))
                .build();
    }

    // -------------------------------------------------------------------------
    // updateUser — happy path
    // -------------------------------------------------------------------------

    @Test
    void shouldUpdateUserAndReturnResponse() {
        Role developerRole = new Role(RoleName.DEVELOPER);
        UserResponse expectedResponse = UserResponse.builder().id(1L).username("mrossi").build();

        when(userRepository.findByIdAndStatusNot(1L, UserStatus.DELETED)).thenReturn(Optional.of(existingUser));
        when(roleRepository.findByName(RoleName.DEVELOPER)).thenReturn(Optional.of(developerRole));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);
        when(userMapper.toResponse(existingUser)).thenReturn(expectedResponse);

        UserResponse result = userService.updateUser(1L, validRequest);

        assertThat(result).isEqualTo(expectedResponse);
        verify(userRepository).save(existingUser);
    }

    @Test
    void shouldUpdateFieldsOnUser() {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .username("gverdi")
                .nome("Giuseppe")
                .cognome("Verdi")
                .roles(Set.of(RoleName.OWNER))
                .build();
        Role ownerRole = new Role(RoleName.OWNER);

        when(userRepository.findByIdAndStatusNot(1L, UserStatus.DELETED)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByUsernameAndIdNot("gverdi", 1L)).thenReturn(false);
        when(roleRepository.findByName(RoleName.OWNER)).thenReturn(Optional.of(ownerRole));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);
        when(userMapper.toResponse(any())).thenReturn(new UserResponse());

        userService.updateUser(1L, request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("gverdi");
        assertThat(saved.getNome()).isEqualTo("Giuseppe");
        assertThat(saved.getCognome()).isEqualTo("Verdi");
        assertThat(saved.getRoles()).containsExactly(ownerRole);
    }

    @Test
    void shouldAllowUpdateWithSameUsername() {
        // username invariato: il controllo di unicità non deve scattare
        when(userRepository.findByIdAndStatusNot(1L, UserStatus.DELETED)).thenReturn(Optional.of(existingUser));
        when(roleRepository.findByName(RoleName.DEVELOPER)).thenReturn(Optional.of(new Role(RoleName.DEVELOPER)));
        when(userRepository.save(any())).thenReturn(existingUser);
        when(userMapper.toResponse(any())).thenReturn(new UserResponse());

        userService.updateUser(1L, validRequest);

        verify(userRepository, never()).existsByUsernameAndIdNot(any(), any());
    }

    // -------------------------------------------------------------------------
    // updateUser — casi di errore
    // -------------------------------------------------------------------------

    @Test
    void shouldThrowWhenUserNotFoundOnUpdate() {
        when(userRepository.findByIdAndStatusNot(99L, UserStatus.DELETED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(99L, validRequest))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("99");

        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenNewUsernameIsTakenByAnotherUser() {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .username("altro_utente")
                .nome("Mario")
                .cognome("Rossi")
                .roles(Set.of(RoleName.DEVELOPER))
                .build();

        when(userRepository.findByIdAndStatusNot(1L, UserStatus.DELETED)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByUsernameAndIdNot("altro_utente", 1L)).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUser(1L, request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("username");

        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenRoleNotFoundOnUpdate() {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .username("mrossi")
                .nome("Mario")
                .cognome("Rossi")
                .roles(Set.of(RoleName.OWNER))
                .build();

        when(userRepository.findByIdAndStatusNot(1L, UserStatus.DELETED)).thenReturn(Optional.of(existingUser));
        when(roleRepository.findByName(RoleName.OWNER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(1L, request))
                .isInstanceOf(RoleNotFoundException.class)
                .hasMessageContaining("OWNER");

        verify(userRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // disableUser
    // -------------------------------------------------------------------------

    @Test
    void shouldDisableUser() {
        when(userRepository.findByIdAndStatusNot(1L, UserStatus.DELETED)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        userService.disableUser(1L);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(UserStatus.DISABLED);
    }

    @Test
    void shouldThrowWhenUserNotFoundOnDisable() {
        when(userRepository.findByIdAndStatusNot(99L, UserStatus.DELETED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.disableUser(99L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("99");

        verify(userRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // enableUser
    // -------------------------------------------------------------------------

    @Test
    void shouldEnableUser() {
        existingUser.setStatus(UserStatus.DISABLED);
        when(userRepository.findByIdAndStatusNot(1L, UserStatus.DELETED)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        userService.enableUser(1L);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void shouldThrowWhenUserNotFoundOnEnable() {
        when(userRepository.findByIdAndStatusNot(99L, UserStatus.DELETED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.enableUser(99L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("99");

        verify(userRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // deleteUser
    // -------------------------------------------------------------------------

    @Test
    void shouldSoftDeleteUser() {
        when(userRepository.findByIdAndStatusNot(1L, UserStatus.DELETED)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        userService.deleteUser(1L);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(UserStatus.DELETED);
    }

    @Test
    void shouldThrowWhenUserNotFoundOnDelete() {
        when(userRepository.findByIdAndStatusNot(99L, UserStatus.DELETED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(99L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("99");

        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenUserAlreadyDeleted() {
        // Un utente con status DELETED non è trovato da findByIdAndStatusNot(DELETED)
        when(userRepository.findByIdAndStatusNot(1L, UserStatus.DELETED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(1L))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository, never()).save(any());
    }
}
