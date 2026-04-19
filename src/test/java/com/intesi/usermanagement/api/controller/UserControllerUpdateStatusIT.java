package com.intesi.usermanagement.api.controller;

import com.intesi.usermanagement.AbstractIntegrationTest;
import com.intesi.usermanagement.domain.enums.RoleName;
import com.intesi.usermanagement.domain.enums.UserStatus;
import com.intesi.usermanagement.dto.request.CreateUserRequest;
import com.intesi.usermanagement.dto.request.UpdateUserRequest;
import com.intesi.usermanagement.dto.response.UserResponse;
import com.intesi.usermanagement.infrastructure.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserControllerUpdateStatusIT extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    // -------------------------------------------------------------------------
    // PUT /users/{id}
    // -------------------------------------------------------------------------

    @Test
    void shouldUpdateUserAndReturn200() {
        UserResponse created = createUser("mrossi", "mario.rossi@example.com", "RSSMRA85M01H501Z");

        UpdateUserRequest updateRequest = UpdateUserRequest.builder()
                .username("mrossi_new")
                .nome("Mario Updated")
                .cognome("Rossi Updated")
                .roles(Set.of(RoleName.OWNER))
                .build();

        ResponseEntity<UserResponse> response = restTemplate.exchange(
                "/users/{id}", HttpMethod.PUT,
                new HttpEntity<>(updateRequest), UserResponse.class, created.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        UserResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getUsername()).isEqualTo("mrossi_new");
        assertThat(body.getNome()).isEqualTo("Mario Updated");
        assertThat(body.getCognome()).isEqualTo("Rossi Updated");
        assertThat(body.getRoles()).containsExactly(RoleName.OWNER);
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentUser() {
        UpdateUserRequest updateRequest = UpdateUserRequest.builder()
                .username("ghost")
                .nome("Ghost")
                .cognome("User")
                .roles(Set.of(RoleName.DEVELOPER))
                .build();

        ResponseEntity<Map> response = restTemplate.exchange(
                "/users/{id}", HttpMethod.PUT,
                new HttpEntity<>(updateRequest), Map.class, 9999L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldReturn409WhenUpdateCausesUsernameConflict() {
        createUser("mrossi", "mario.rossi@example.com", "RSSMRA85M01H501Z");
        UserResponse second = createUser("gverdi", "giuseppe.verdi@example.com", "VRDGPP80A01H501X");

        UpdateUserRequest conflictRequest = UpdateUserRequest.builder()
                .username("mrossi")          // già usato dal primo utente
                .nome("Giuseppe")
                .cognome("Verdi")
                .roles(Set.of(RoleName.DEVELOPER))
                .build();

        ResponseEntity<Map> response = restTemplate.exchange(
                "/users/{id}", HttpMethod.PUT,
                new HttpEntity<>(conflictRequest), Map.class, second.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void shouldReturn400WhenUpdateRequestIsInvalid() {
        UserResponse created = createUser("mrossi", "mario.rossi@example.com", "RSSMRA85M01H501Z");

        UpdateUserRequest invalid = UpdateUserRequest.builder()
                .username("")               // blank: violazione @NotBlank
                .nome("Mario")
                .cognome("Rossi")
                .roles(Set.of(RoleName.DEVELOPER))
                .build();

        ResponseEntity<Map> response = restTemplate.exchange(
                "/users/{id}", HttpMethod.PUT,
                new HttpEntity<>(invalid), Map.class, created.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // -------------------------------------------------------------------------
    // PATCH /users/{id}/disable
    // -------------------------------------------------------------------------

    @Test
    void shouldDisableUserAndReturn204() {
        UserResponse created = createUser("mrossi", "mario.rossi@example.com", "RSSMRA85M01H501Z");

        ResponseEntity<Void> response = restTemplate.exchange(
                "/users/{id}/disable", HttpMethod.POST,
                HttpEntity.EMPTY, Void.class, created.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verifica che lo stato sia effettivamente DISABLED
        ResponseEntity<UserResponse> getResponse = restTemplate.getForEntity(
                "/users/{id}", UserResponse.class, created.getId());
        assertThat(getResponse.getBody().getStatus()).isEqualTo(UserStatus.DISABLED);
    }

    @Test
    void shouldReturn404WhenDisablingNonExistentUser() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/users/{id}/disable", HttpMethod.POST,
                HttpEntity.EMPTY, Map.class, 9999L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // -------------------------------------------------------------------------
    // PATCH /users/{id}/enable
    // -------------------------------------------------------------------------

    @Test
    void shouldEnableDisabledUserAndReturn204() {
        UserResponse created = createUser("mrossi", "mario.rossi@example.com", "RSSMRA85M01H501Z");

        // Prima disabilita
        restTemplate.exchange("/users/{id}/disable", HttpMethod.POST,
                HttpEntity.EMPTY, Void.class, created.getId());

        // Poi riabilita
        ResponseEntity<Void> response = restTemplate.exchange(
                "/users/{id}/enable", HttpMethod.POST,
                HttpEntity.EMPTY, Void.class, created.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<UserResponse> getResponse = restTemplate.getForEntity(
                "/users/{id}", UserResponse.class, created.getId());
        assertThat(getResponse.getBody().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void shouldReturn404WhenEnablingNonExistentUser() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/users/{id}/enable", HttpMethod.POST,
                HttpEntity.EMPTY, Map.class, 9999L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // -------------------------------------------------------------------------
    // DELETE /users/{id}
    // -------------------------------------------------------------------------

    @Test
    void shouldDeleteUserAndReturn204() {
        UserResponse created = createUser("mrossi", "mario.rossi@example.com", "RSSMRA85M01H501Z");

        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                "/users/{id}", HttpMethod.DELETE,
                HttpEntity.EMPTY, Void.class, created.getId());

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verifica che non sia più recuperabile
        ResponseEntity<Map> getResponse = restTemplate.getForEntity(
                "/users/{id}", Map.class, created.getId());
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentUser() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/users/{id}", HttpMethod.DELETE,
                HttpEntity.EMPTY, Map.class, 9999L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldExcludeDeletedUserFromList() {
        UserResponse created = createUser("mrossi", "mario.rossi@example.com", "RSSMRA85M01H501Z");

        restTemplate.exchange("/users/{id}", HttpMethod.DELETE,
                HttpEntity.EMPTY, Void.class, created.getId());

        ResponseEntity<Map> listResponse = restTemplate.getForEntity("/users", Map.class);
        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((java.util.List<?>) listResponse.getBody().get("content")).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldIncludeDisabledUserInList() {
        UserResponse created = createUser("mrossi", "mario.rossi@example.com", "RSSMRA85M01H501Z");

        restTemplate.exchange("/users/{id}/disable", HttpMethod.POST,
                HttpEntity.EMPTY, Void.class, created.getId());

        ResponseEntity<Map> listResponse = restTemplate.getForEntity("/users", Map.class);
        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((java.util.List<?>) listResponse.getBody().get("content")).hasSize(1);
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private UserResponse createUser(String username, String email, String codiceFiscale) {
        CreateUserRequest request = CreateUserRequest.builder()
                .username(username)
                .email(email)
                .codiceFiscale(codiceFiscale)
                .nome("Mario")
                .cognome("Rossi")
                .roles(Set.of(RoleName.DEVELOPER))
                .build();
        return restTemplate.postForEntity("/users", request, UserResponse.class).getBody();
    }
}
