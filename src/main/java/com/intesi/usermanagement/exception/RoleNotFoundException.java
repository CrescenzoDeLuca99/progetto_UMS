package com.intesi.usermanagement.exception;

import com.intesi.usermanagement.domain.enums.RoleName;

public class RoleNotFoundException extends RuntimeException {

    public RoleNotFoundException(RoleName roleName) {
        super("Ruolo non trovato: " + roleName);
    }
}
