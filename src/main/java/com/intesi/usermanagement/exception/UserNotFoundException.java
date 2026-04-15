package com.intesi.usermanagement.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long id) {
        super("Utente non trovato con id: " + id);
    }
}
