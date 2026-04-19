package com.intesi.usermanagement.api.controller;

import com.intesi.usermanagement.AbstractIntegrationTest;
import com.intesi.usermanagement.domain.enums.RoleName;
import com.intesi.usermanagement.dto.request.CreateUserRequest;
import com.intesi.usermanagement.dto.response.UserResponse;
import com.intesi.usermanagement.infrastructure.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserControllerCreateReadIT extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    // -------------------------------------------------------------------------
    // POST /users
    // -------------------------------------------------------------------------

    @Test
    void shouldCreateUserAndReturn201() {
        CreateUserRequest request = buildRequest("mrossi", "mario.rossi@example.com", "RSSMRA85M01H501Z");

        ResponseEntity<UserResponse> response = restTemplate.postForEntity("/users", request, UserResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).isNotNull();
        UserResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getId()).isNotNull();
        assertThat(body.getUsername()).isEqualTo("mrossi");
        assertThat(body.getEmail()).isEqualTo("mario.rossi@example.com");
        assertThat(body.getRoles()).containsExactly(RoleName.DEVELOPER);
    }

    @Test
    void shouldReturn409WhenUsernameAlreadyExists() {
        CreateUserRequest first = buildRequest("mrossi", "mario.rossi@example.com", "RSSMRA85M01H501Z");
        CreateUserRequest duplicate = buildRequest("mrossi", "altro@example.com", "VRDGPP80A01H501X");

        restTemplate.postForEntity("/users", first, UserResponse.class);
        ResponseEntity<Map> response = restTemplate.postForEntity("/users", duplicate, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void shouldReturn400WhenRequestIsInvalid() {
        CreateUserRequest invalid = CreateUserRequest.builder()
                .username("")
                .email("non-una-email")
                .codiceFiscale("INVALIDO")
                .nome("Mario")
                .cognome("Rossi")
                .roles(Set.of(RoleName.DEVELOPER))
                .build();

        ResponseEntity<Map> response = restTemplate.postForEntity("/users", invalid, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // -------------------------------------------------------------------------
    // GET /users/{id}
    // -------------------------------------------------------------------------

    @Test
    void shouldReturnUserById() {
        CreateUserRequest request = buildRequest("mrossi", "mario.rossi@example.com", "RSSMRA85M01H501Z");
        UserResponse created = restTemplate.postForEntity("/users", request, UserResponse.class).getBody();

        ResponseEntity<UserResponse> response = restTemplate.getForEntity("/users/{id}", UserResponse.class, created.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getUsername()).isEqualTo("mrossi");
    }

    @Test
    void shouldReturn404WhenUserNotFound() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/users/{id}", Map.class, 9999L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // -------------------------------------------------------------------------
    // GET /users
    // -------------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnPagedListOfUsers() {
        restTemplate.postForEntity("/users", buildRequest("mrossi", "mario.rossi@example.com", "RSSMRA85M01H501Z"), UserResponse.class);
        restTemplate.postForEntity("/users", buildRequest("gverdi", "giuseppe.verdi@example.com", "VRDGPP80A01H501X"), UserResponse.class);

        ResponseEntity<Map> response = restTemplate.getForEntity("/users", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> page = response.getBody();
        assertThat((java.util.List<?>) page.get("content")).hasSize(2);
        assertThat((Integer) page.get("totalElements")).isEqualTo(2);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnEmptyPageWhenNoUsers() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/users", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> page = response.getBody();
        assertThat((java.util.List<?>) page.get("content")).isEmpty();
        assertThat((Integer) page.get("totalElements")).isEqualTo(0);
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private CreateUserRequest buildRequest(String username, String email, String codiceFiscale) {
        return CreateUserRequest.builder()
                .username(username)
                .email(email)
                .codiceFiscale(codiceFiscale)
                .nome("Mario")
                .cognome("Rossi")
                .roles(Set.of(RoleName.DEVELOPER))
                .build();
    }
}
