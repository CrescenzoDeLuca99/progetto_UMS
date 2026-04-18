package com.intesi.usermanagement.api.controller;

import com.intesi.usermanagement.AbstractIntegrationTest;
import com.intesi.usermanagement.domain.enums.RoleName;
import com.intesi.usermanagement.dto.request.CreateUserRequest;
import com.intesi.usermanagement.dto.request.UpdateUserRequest;
import com.intesi.usermanagement.dto.response.UserResponse;
import com.intesi.usermanagement.infrastructure.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserControllerRbacIT extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    private Long existingUserId;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        // configureAuth() (parent) ha già impostato il token OWNER: si può creare l'utente di fixture
        UserResponse created = restTemplate
                .postForEntity("/users", buildRequest("rbac-fixture"), UserResponse.class)
                .getBody();
        existingUserId = created.getId();
    }

    // -------------------------------------------------------------------------
    // REPORTER — può solo leggere
    // -------------------------------------------------------------------------

    @Test
    void reporterCanReadUser() {
        setTokenRoles(restTemplate, List.of("REPORTER"));

        ResponseEntity<UserResponse> response =
                restTemplate.getForEntity("/users/{id}", UserResponse.class, existingUserId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void reporterCannotCreateUser() {
        setTokenRoles(restTemplate, List.of("REPORTER"));

        ResponseEntity<Map> response =
                restTemplate.postForEntity("/users", buildRequest("new-user"), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void reporterCannotUpdateUser() {
        setTokenRoles(restTemplate, List.of("REPORTER"));

        UpdateUserRequest update = UpdateUserRequest.builder()
                .username("changed")
                .nome("Nome")
                .cognome("Cognome")
                .roles(Set.of(RoleName.DEVELOPER))
                .build();

        ResponseEntity<Map> response = restTemplate.exchange(
                "/users/{id}", HttpMethod.PUT,
                new HttpEntity<>(update), Map.class, existingUserId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void reporterCannotDeleteUser() {
        setTokenRoles(restTemplate, List.of("REPORTER"));

        ResponseEntity<Map> response = restTemplate.exchange(
                "/users/{id}", HttpMethod.DELETE,
                HttpEntity.EMPTY, Map.class, existingUserId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // -------------------------------------------------------------------------
    // DEVELOPER — può solo leggere
    // -------------------------------------------------------------------------

    @Test
    void developerCannotDisableUser() {
        setTokenRoles(restTemplate, List.of("DEVELOPER"));

        ResponseEntity<Map> response = restTemplate.exchange(
                "/users/{id}/disable", HttpMethod.POST,
                HttpEntity.EMPTY, Map.class, existingUserId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void developerCannotDeleteUser() {
        setTokenRoles(restTemplate, List.of("DEVELOPER"));

        ResponseEntity<Map> response = restTemplate.exchange(
                "/users/{id}", HttpMethod.DELETE,
                HttpEntity.EMPTY, Map.class, existingUserId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // -------------------------------------------------------------------------
    // MAINTAINER — può creare e modificare, ma non cancellare né gestire lo stato
    // -------------------------------------------------------------------------

    @Test
    void maintainerCanCreateUser() {
        setTokenRoles(restTemplate, List.of("MAINTAINER"));

        ResponseEntity<UserResponse> response =
                restTemplate.postForEntity("/users", buildRequest("maintainer-created"), UserResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void maintainerCannotDeleteUser() {
        setTokenRoles(restTemplate, List.of("MAINTAINER"));

        ResponseEntity<Map> response = restTemplate.exchange(
                "/users/{id}", HttpMethod.DELETE,
                HttpEntity.EMPTY, Map.class, existingUserId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void maintainerCannotDisableUser() {
        setTokenRoles(restTemplate, List.of("MAINTAINER"));

        ResponseEntity<Map> response = restTemplate.exchange(
                "/users/{id}/disable", HttpMethod.POST,
                HttpEntity.EMPTY, Map.class, existingUserId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // -------------------------------------------------------------------------
    // OPERATOR — può creare, modificare e gestire lo stato, ma non cancellare
    // -------------------------------------------------------------------------

    @Test
    void operatorCanDisableUser() {
        setTokenRoles(restTemplate, List.of("OPERATOR"));

        ResponseEntity<Void> response = restTemplate.exchange(
                "/users/{id}/disable", HttpMethod.POST,
                HttpEntity.EMPTY, Void.class, existingUserId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void operatorCannotDeleteUser() {
        setTokenRoles(restTemplate, List.of("OPERATOR"));

        ResponseEntity<Map> response = restTemplate.exchange(
                "/users/{id}", HttpMethod.DELETE,
                HttpEntity.EMPTY, Map.class, existingUserId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private static final Map<String, String> CODICI_FISCALI = Map.of(
            "rbac-fixture",       "RSSMRA85M01H501Z",
            "maintainer-created", "VRDGPP80A01H501X",
            "new-user",           "BNCLCU80A41F205C"
    );

    private CreateUserRequest buildRequest(String username) {
        return CreateUserRequest.builder()
                .username(username)
                .email(username + "@example.com")
                .codiceFiscale(CODICI_FISCALI.get(username))
                .nome("Nome")
                .cognome("Cognome")
                .roles(Set.of(RoleName.DEVELOPER))
                .build();
    }
}
