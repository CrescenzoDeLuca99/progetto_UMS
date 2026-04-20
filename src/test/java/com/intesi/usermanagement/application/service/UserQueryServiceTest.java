package com.intesi.usermanagement.application.service;

import com.intesi.usermanagement.application.port.out.UserPersistencePort;
import com.intesi.usermanagement.domain.enums.RoleName;
import com.intesi.usermanagement.domain.enums.UserStatus;
import com.intesi.usermanagement.domain.model.Role;
import com.intesi.usermanagement.domain.model.User;
import com.intesi.usermanagement.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserQueryServiceTest {

    @Mock
    private UserPersistencePort userPersistence;

    @InjectMocks
    private UserQueryService userQueryService;

    private User activeUser;

    @BeforeEach
    void setUp() {
        activeUser = User.builder()
                .id(1L)
                .username("mrossi")
                .email("mario.rossi@example.com")
                .codiceFiscale("RSSMRA85M01H501Z")
                .nome("Mario")
                .cognome("Rossi")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(new Role(RoleName.DEVELOPER)))
                .build();
    }

    // -------------------------------------------------------------------------
    // getUserById
    // -------------------------------------------------------------------------

    @Test
    void shouldReturnUserWhenFound() {
        when(userPersistence.findByIdAndStatusNot(1L, UserStatus.DELETED)).thenReturn(Optional.of(activeUser));

        User result = userQueryService.getUserById(1L);

        assertThat(result).isEqualTo(activeUser);
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        when(userPersistence.findByIdAndStatusNot(99L, UserStatus.DELETED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userQueryService.getUserById(99L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void shouldThrowWhenUserIsDeleted() {
        when(userPersistence.findByIdAndStatusNot(1L, UserStatus.DELETED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userQueryService.getUserById(1L))
                .isInstanceOf(UserNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // listUsers
    // -------------------------------------------------------------------------

    @Test
    void shouldReturnPageOfUsers() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<User> page = new PageImpl<>(List.of(activeUser), pageable, 1);
        when(userPersistence.findAllExcludingStatus(UserStatus.DELETED, pageable)).thenReturn(page);

        Page<User> result = userQueryService.listUsers(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).containsExactly(activeUser);
    }

    @Test
    void shouldReturnEmptyPageWhenNoUsers() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<User> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        when(userPersistence.findAllExcludingStatus(UserStatus.DELETED, pageable)).thenReturn(emptyPage);

        Page<User> result = userQueryService.listUsers(pageable);

        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getContent()).isEmpty();
    }
}
