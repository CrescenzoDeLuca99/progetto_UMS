package com.intesi.usermanagement.exception;

public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException(String field, String value) {
        super("Utente già esistente con " + field + ": " + value);
    }
}
