package com.intesi.usermanagement.application.service;

import com.intesi.usermanagement.application.port.out.RolePersistencePort;
import com.intesi.usermanagement.application.port.out.UserEventPort;
import com.intesi.usermanagement.application.port.out.UserPersistencePort;
import com.intesi.usermanagement.domain.enums.RoleName;
import com.intesi.usermanagement.domain.enums.UserEventType;
import com.intesi.usermanagement.domain.enums.UserStatus;
import com.intesi.usermanagement.domain.model.Role;
import com.intesi.usermanagement.domain.model.User;
import com.intesi.usermanagement.dto.request.UpdateUserRequest;
import com.intesi.usermanagement.exception.RoleNotFoundException;
import com.intesi.usermanagement.exception.UserAlreadyExistsException;
import com.intesi.usermanagement.exception.UserNotFoundException;
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
    private UserPersistencePort userPersistence;

    @Mock
    private RolePersistencePort rolePersistence;

    @Mock
    private UserEventPort eventPort;

    @InjectMocks
    private UserCommandService userService;

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

        when(userPersistence.findByIdAndStatusNot(1L, UserStatus.DELETED)).thenReturn(Optional.of(existingUser));
        when(rolePersistence.findByName(RoleName.DEVELOPER)).thenReturn(Optional.of(developerRole));
        when(userPersistence.save(any(User.class))).thenReturn(existingUser);

        User result = userService.updateUser(1L, validRequest);

        assertThat(result).isEqualTo(existingUser);
        verify(userPersistence).save(existingUser);
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

        when(userPersistence.findByIdAndStatusNot(1L, UserStatus.DELETED)).thenReturn(Optional.of(existingUser));
        when(userPersistence.existsByUsernameAndIdNot("gverdi", 1L)).thenReturn(false);
        when(rolePersistence.findByName(RoleName.OWNER)).thenReturn(Optional.of(ownerRole));
        when(userPersistence.save(any(User.class))).thenReturn(existingUser);

        userService.updateUser(1L, request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userPersistence).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("gverdi");
        assertThat(saved.getNome()).isEqualTo("Giuseppe");
        assertThat(saved.getCognome()).isEqualTo("Verdi");
        assertThat(saved.getRoles()).containsExactly(ownerRole);
    }

    @Test
    void shouldAllowUpdateWithSameUsername() {
        // username invariato: il controllo di unicità non deve scattare
        when(userPersistence.findByIdAndStatusNot(1L, UserStatus.DELETED)).thenReturn(Optional.of(existingUser));
        when(rolePersistence.findByName(RoleName.DEVELOPER)).thenReturn(Optional.of(new Role(RoleName.DEVELOPER)));
        when(userPersistence.save(any())).thenReturn(existingUser);

        userService.updateUser(1L, validRequest);

        verify(userPersistence, never()).existsByUsernameAndIdNot(any(), any());
    }

    // -------------------------------------------------------------------------
    // updateUser — casi di errore
    // -------------------------------------------------------------------------

    @Test
    void shouldThrowWhenUserNotFoundOnUpdate() {
        when(userPersistence.findByIdAndStatusNot(99L, UserStatus.DELETED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(99L, validRequest))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("99");

        verify(userPersistence, never()).save(any());
    }

    @Test
    void shouldThrowWhenNewUsernameIsTakenByAnotherUser() {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .username("altro_utente")
                .nome("Mario")
                .cognome("Rossi")
                .roles(Set.of(RoleName.DEVELOPER))
                .build();

        when(userPersistence.findByIdAndStatusNot(1L, UserStatus.DELETED)).thenReturn(Optional.of(existingUser));
        when(userPersistence.existsByUsernameAndIdNot("altro_utente", 1L)).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUser(1L, request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("username");

        verify(userPersistence, never()).save(any());
    }

    @Test
    void shouldThrowWhenRoleNotFoundOnUpdate() {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .username("mrossi")
                .nome("Mario")
                .cognome("Rossi")
                .roles(Set.of(RoleName.OWNER))
                .build();

        when(userPersistence.findByIdAndStatusNot(1L, UserStatus.DELETED)).thenReturn(Optional.of(existingUser));
        when(rolePersistence.findByName(RoleName.OWNER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(1L, request))
                .isInstanceOf(RoleNotFoundException.class)
                .hasMessageContaining("OWNER");

        verify(userPersistence, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // disableUser
    // -------------------------------------------------------------------------

    @Test
    void shouldDisableUser() {
        when(userPersistence.findByIdAndStatusNot(1L, UserStatus.DELETED)).thenReturn(Optional.of(existingUser));
        when(userPersistence.save(any(User.class))).thenReturn(existingUser);

        userService.disableUser(1L);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userPersistence).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(UserStatus.DISABLED);
    }

    @Test
    void shouldThrowWhenUserNotFoundOnDisable() {
        when(userPersistence.findByIdAndStatusNot(99L, UserStatus.DELETED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.disableUser(99L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("99");

        verify(userPersistence, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // enableUser
    // -------------------------------------------------------------------------

    @Test
    void shouldEnableUser() {
        existingUser.setStatus(UserStatus.DISABLED);
        when(userPersistence.findByIdAndStatusNot(1L, UserStatus.DELETED)).thenReturn(Optional.of(existingUser));
        when(userPersistence.save(any(User.class))).thenReturn(existingUser);

        userService.enableUser(1L);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userPersistence).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void shouldThrowWhenUserNotFoundOnEnable() {
        when(userPersistence.findByIdAndStatusNot(99L, UserStatus.DELETED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.enableUser(99L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("99");

        verify(userPersistence, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // deleteUser
    // -------------------------------------------------------------------------

    @Test
    void shouldSoftDeleteUser() {
        when(userPersistence.findByIdAndStatusNot(1L, UserStatus.DELETED)).thenReturn(Optional.of(existingUser));
        when(userPersistence.save(any(User.class))).thenReturn(existingUser);

        userService.deleteUser(1L);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userPersistence).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(UserStatus.DELETED);
    }

    @Test
    void shouldThrowWhenUserNotFoundOnDelete() {
        when(userPersistence.findByIdAndStatusNot(99L, UserStatus.DELETED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(99L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("99");

        verify(userPersistence, never()).save(any());
    }

    @Test
    void shouldThrowWhenUserAlreadyDeleted() {
        when(userPersistence.findByIdAndStatusNot(1L, UserStatus.DELETED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(1L))
                .isInstanceOf(UserNotFoundException.class);

        verify(userPersistence, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // Pubblicazione eventi
    // -------------------------------------------------------------------------

    @Test
    void shouldPublishUpdatedEvent() {
        when(userPersistence.findByIdAndStatusNot(1L, UserStatus.DELETED)).thenReturn(Optional.of(existingUser));
        when(rolePersistence.findByName(RoleName.DEVELOPER)).thenReturn(Optional.of(new Role(RoleName.DEVELOPER)));
        when(userPersistence.save(any())).thenReturn(existingUser);

        userService.updateUser(1L, validRequest);

        verify(eventPort).publish(UserEventType.UPDATED, existingUser);
    }

    @Test
    void shouldPublishDisabledEvent() {
        when(userPersistence.findByIdAndStatusNot(1L, UserStatus.DELETED)).thenReturn(Optional.of(existingUser));
        when(userPersistence.save(any())).thenReturn(existingUser);

        userService.disableUser(1L);

        verify(eventPort).publish(UserEventType.DISABLED, existingUser);
    }

    @Test
    void shouldPublishEnabledEvent() {
        when(userPersistence.findByIdAndStatusNot(1L, UserStatus.DELETED)).thenReturn(Optional.of(existingUser));
        when(userPersistence.save(any())).thenReturn(existingUser);

        userService.enableUser(1L);

        verify(eventPort).publish(UserEventType.ENABLED, existingUser);
    }

    @Test
    void shouldPublishDeletedEvent() {
        when(userPersistence.findByIdAndStatusNot(1L, UserStatus.DELETED)).thenReturn(Optional.of(existingUser));
        when(userPersistence.save(any())).thenReturn(existingUser);

        userService.deleteUser(1L);

        verify(eventPort).publish(UserEventType.DELETED, existingUser);
    }
}
