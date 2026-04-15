package com.intesi.usermanagement.application.service;

import com.intesi.usermanagement.domain.enums.UserStatus;
import com.intesi.usermanagement.domain.model.Role;
import com.intesi.usermanagement.domain.model.User;
import com.intesi.usermanagement.dto.request.CreateUserRequest;
import com.intesi.usermanagement.dto.request.UpdateUserRequest;
import com.intesi.usermanagement.dto.response.UserResponse;
import com.intesi.usermanagement.exception.RoleNotFoundException;
import com.intesi.usermanagement.exception.UserAlreadyExistsException;
import com.intesi.usermanagement.exception.UserNotFoundException;
import com.intesi.usermanagement.infrastructure.repository.RoleRepository;
import com.intesi.usermanagement.infrastructure.repository.UserRepository;
import com.intesi.usermanagement.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("username", request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("email", request.getEmail());
        }
        if (userRepository.existsByCodiceFiscale(request.getCodiceFiscale())) {
            throw new UserAlreadyExistsException("codiceFiscale", request.getCodiceFiscale());
        }

        Set<Role> roles = resolveRoles(request);

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .codiceFiscale(request.getCodiceFiscale().toUpperCase())
                .nome(request.getNome())
                .cognome(request.getCognome())
                .roles(roles)
                .build();

        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findByIdAndStatusNot(id, UserStatus.DELETED)
                .orElseThrow(() -> new UserNotFoundException(id));
        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(Pageable pageable) {
        return userRepository.findAllExcludingStatus(UserStatus.DELETED, pageable)
                .map(userMapper::toResponse);
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findByIdAndStatusNot(id, UserStatus.DELETED)
                .orElseThrow(() -> new UserNotFoundException(id));

        if (!user.getUsername().equals(request.getUsername()) &&
                userRepository.existsByUsernameAndIdNot(request.getUsername(), id)) {
            throw new UserAlreadyExistsException("username", request.getUsername());
        }

        Set<Role> roles = request.getRoles().stream()
                .map(roleName -> roleRepository.findByName(roleName)
                        .orElseThrow(() -> new RoleNotFoundException(roleName)))
                .collect(Collectors.toSet());

        user.setUsername(request.getUsername());
        user.setNome(request.getNome());
        user.setCognome(request.getCognome());
        user.setRoles(roles);

        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public void disableUser(Long id) {
        User user = userRepository.findByIdAndStatusNot(id, UserStatus.DELETED)
                .orElseThrow(() -> new UserNotFoundException(id));
        user.setStatus(UserStatus.DISABLED);
        userRepository.save(user);
    }

    @Transactional
    public void enableUser(Long id) {
        User user = userRepository.findByIdAndStatusNot(id, UserStatus.DELETED)
                .orElseThrow(() -> new UserNotFoundException(id));
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findByIdAndStatusNot(id, UserStatus.DELETED)
                .orElseThrow(() -> new UserNotFoundException(id));
        user.setStatus(UserStatus.DELETED);
        userRepository.save(user);
    }

    private Set<Role> resolveRoles(CreateUserRequest request) {
        return request.getRoles().stream()
                .map(roleName -> roleRepository.findByName(roleName)
                        .orElseThrow(() -> new RoleNotFoundException(roleName)))
                .collect(Collectors.toSet());
    }
}
