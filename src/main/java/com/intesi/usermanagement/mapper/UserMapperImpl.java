package com.intesi.usermanagement.mapper;

import com.intesi.usermanagement.domain.enums.RoleName;
import com.intesi.usermanagement.domain.model.Role;
import com.intesi.usermanagement.domain.model.User;
import com.intesi.usermanagement.dto.response.UserResponse;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .codiceFiscale(user.getCodiceFiscale())
                .nome(user.getNome())
                .cognome(user.getCognome())
                .status(user.getStatus())
                .roles(toRoleNames(user.getRoles()))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private Set<RoleName> toRoleNames(Set<Role> roles) {
        if (roles == null) return Set.of();
        return roles.stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
    }
}
