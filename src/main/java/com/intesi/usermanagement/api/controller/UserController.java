package com.intesi.usermanagement.api.controller;

import com.intesi.usermanagement.application.port.in.UserCommandUseCase;
import com.intesi.usermanagement.application.port.in.UserQueryUseCase;
import com.intesi.usermanagement.domain.model.User;
import com.intesi.usermanagement.dto.request.CreateUserRequest;
import com.intesi.usermanagement.dto.request.UpdateUserRequest;
import com.intesi.usermanagement.dto.response.UserResponse;
import com.intesi.usermanagement.mapper.UserMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserCommandUseCase commandUseCase;
    private final UserQueryUseCase queryUseCase;
    private final UserMapper userMapper;

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','OPERATOR','MAINTAINER')")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        User created = commandUseCase.createUser(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(userMapper.toResponse(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userMapper.toResponse(queryUseCase.getUserById(id)));
    }

    @GetMapping
    public ResponseEntity<Page<UserResponse>> listUsers(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(queryUseCase.listUsers(pageable).map(userMapper::toResponse));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','OPERATOR','MAINTAINER')")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userMapper.toResponse(commandUseCase.updateUser(id, request)));
    }

    @PostMapping("/{id}/disable")
    @PreAuthorize("hasAnyRole('OWNER','OPERATOR')")
    public ResponseEntity<Void> disableUser(@PathVariable Long id) {
        commandUseCase.disableUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/enable")
    @PreAuthorize("hasAnyRole('OWNER','OPERATOR')")
    public ResponseEntity<Void> enableUser(@PathVariable Long id) {
        commandUseCase.enableUser(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        commandUseCase.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
