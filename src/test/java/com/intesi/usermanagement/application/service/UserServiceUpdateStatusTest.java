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
class UserServiceUpdateStatusTest {

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

        when(userDao.findByIdAndStatusNot(1L, UserStatus.DELETED)).thenReturn(Optional.of(existingUser));
        when(roleDao.findByName(RoleName.DEVELOPER)).thenReturn(Optional.of(developerRole));
        when(userDao.save(any(User.class))).thenReturn(existingUser);
        when(userMapper.toResponse(existingUser)).thenReturn(expectedResponse);

        UserResponse result = userService.updateUser(1L, validRequest);

        assertThat(result).isEqualTo(expectedResponse);
        verify(userDao).save(existingUser);
    }

    @Test
    void shouldUpdateFieldsOnUser() {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .username("gverdi")
                .nome("Giuseppe")
                .cognome("Verdi")
                .roles(Set.of(RoleName.ADMIN))
                .build();
        Role ownerRole = new Role(RoleName.ADMIN);

        when(userDao.findByIdAndStatusNot(1L, UserStatus.DELETED)).thenReturn(Optional.of(existingUser));
        when(userDao.existsByUsernameAndIdNot("gverdi", 1L)).thenReturn(false);
        when(roleDao.findByName(RoleName.ADMIN)).thenReturn(Optional.of(ownerRole));
        when(userDao.save(any(User.class))).thenReturn(existingUser);
        when(userMapper.toResponse(any())).thenReturn(new UserResponse());

        userService.updateUser(1L, request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userDao).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("gverdi");
        assertThat(saved.getNome()).isEqualTo("Giuseppe");
        assertThat(saved.getCognome()).isEqualTo("Verdi");
        assertThat(saved.getRoles()).containsExactly(ownerRole);
    }

    @Test
    void shouldAllowUpdateWithSameUsername() {
        // username invariato: il controllo di unicità non deve scattare
        when(userDao.findByIdAndStatusNot(1L, UserStatus.DELETED)).thenReturn(Optional.of(existingUser));
        when(roleDao.findByName(RoleName.DEVELOPER)).thenReturn(Optional.of(new Role(RoleName.DEVELOPER)));
        when(userDao.save(any())).thenReturn(existingUser);
        when(userMapper.toResponse(any())).thenReturn(new UserResponse());

        userService.updateUser(1L, validRequest);

        verify(userDao, never()).existsByUsernameAndIdNot(any(), any());
    }

    // -------------------------------------------------------------------------
    // updateUser — casi di errore
    // -------------------------------------------------------------------------

    @Test
    void shouldThrowWhenUserNotFoundOnUpdate() {
        when(userDao.findByIdAndStatusNot(99L, UserStatus.DELETED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(99L, validRequest))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("99");

        verify(userDao, never()).save(any());
    }

    @Test
    void shouldThrowWhenNewUsernameIsTakenByAnotherUser() {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .username("altro_utente")
                .nome("Mario")
                .cognome("Rossi")
                .roles(Set.of(RoleName.DEVELOPER))
                .build();

        when(userDao.findByIdAndStatusNot(1L, UserStatus.DELETED)).thenReturn(Optional.of(existingUser));
        when(userDao.existsByUsernameAndIdNot("altro_utente", 1L)).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUser(1L, request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("username");

        verify(userDao, never()).save(any());
    }

    @Test
    void shouldThrowWhenRoleNotFoundOnUpdate() {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .username("mrossi")
                .nome("Mario")
                .cognome("Rossi")
                .roles(Set.of(RoleName.ADMIN))
                .build();

        when(userDao.findByIdAndStatusNot(1L, UserStatus.DELETED)).thenReturn(Optional.of(existingUser));
        when(roleDao.findByName(RoleName.ADMIN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(1L, request))
                .isInstanceOf(RoleNotFoundException.class)
                .hasMessageContaining("ADMIN");

        verify(userDao, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // disableUser
    // -------------------------------------------------------------------------

    @Test
    void shouldDisableUser() {
        when(userDao.findByIdAndStatusNot(1L, UserStatus.DELETED)).thenReturn(Optional.of(existingUser));
        when(userDao.save(any(User.class))).thenReturn(existingUser);

        userService.disableUser(1L);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userDao).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(UserStatus.DISABLED);
    }

    @Test
    void shouldThrowWhenUserNotFoundOnDisable() {
        when(userDao.findByIdAndStatusNot(99L, UserStatus.DELETED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.disableUser(99L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("99");

        verify(userDao, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // enableUser
    // -------------------------------------------------------------------------

    @Test
    void shouldEnableUser() {
        existingUser.setStatus(UserStatus.DISABLED);
        when(userDao.findByIdAndStatusNot(1L, UserStatus.DELETED)).thenReturn(Optional.of(existingUser));
        when(userDao.save(any(User.class))).thenReturn(existingUser);

        userService.enableUser(1L);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userDao).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void shouldThrowWhenUserNotFoundOnEnable() {
        when(userDao.findByIdAndStatusNot(99L, UserStatus.DELETED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.enableUser(99L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("99");

        verify(userDao, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // deleteUser
    // -------------------------------------------------------------------------

    @Test
    void shouldSoftDeleteUser() {
        when(userDao.findByIdAndStatusNot(1L, UserStatus.DELETED)).thenReturn(Optional.of(existingUser));
        when(userDao.save(any(User.class))).thenReturn(existingUser);

        userService.deleteUser(1L);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userDao).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(UserStatus.DELETED);
    }

    @Test
    void shouldThrowWhenUserNotFoundOnDelete() {
        when(userDao.findByIdAndStatusNot(99L, UserStatus.DELETED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(99L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("99");

        verify(userDao, never()).save(any());
    }

    @Test
    void shouldThrowWhenUserAlreadyDeleted() {
        when(userDao.findByIdAndStatusNot(1L, UserStatus.DELETED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(1L))
                .isInstanceOf(UserNotFoundException.class);

        verify(userDao, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // Pubblicazione eventi
    // -------------------------------------------------------------------------

    @Test
    void shouldPublishUpdatedEvent() {
        when(userDao.findByIdAndStatusNot(1L, UserStatus.DELETED)).thenReturn(Optional.of(existingUser));
        when(roleDao.findByName(RoleName.DEVELOPER)).thenReturn(Optional.of(new Role(RoleName.DEVELOPER)));
        when(userDao.save(any())).thenReturn(existingUser);
        when(userMapper.toResponse(any())).thenReturn(new UserResponse());

        userService.updateUser(1L, validRequest);

        verify(eventPublisher).publish(UserEventType.UPDATED, existingUser);
    }

    @Test
    void shouldPublishDisabledEvent() {
        when(userDao.findByIdAndStatusNot(1L, UserStatus.DELETED)).thenReturn(Optional.of(existingUser));
        when(userDao.save(any())).thenReturn(existingUser);

        userService.disableUser(1L);

        verify(eventPublisher).publish(UserEventType.DISABLED, existingUser);
    }

    @Test
    void shouldPublishEnabledEvent() {
        when(userDao.findByIdAndStatusNot(1L, UserStatus.DELETED)).thenReturn(Optional.of(existingUser));
        when(userDao.save(any())).thenReturn(existingUser);

        userService.enableUser(1L);

        verify(eventPublisher).publish(UserEventType.ENABLED, existingUser);
    }

    @Test
    void shouldPublishDeletedEvent() {
        when(userDao.findByIdAndStatusNot(1L, UserStatus.DELETED)).thenReturn(Optional.of(existingUser));
        when(userDao.save(any())).thenReturn(existingUser);

        userService.deleteUser(1L);

        verify(eventPublisher).publish(UserEventType.DELETED, existingUser);
    }
}
