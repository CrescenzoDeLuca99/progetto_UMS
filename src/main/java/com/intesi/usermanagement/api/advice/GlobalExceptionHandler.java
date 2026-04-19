package com.intesi.usermanagement.api.advice;

import com.intesi.usermanagement.exception.RoleNotFoundException;
import com.intesi.usermanagement.exception.UserAlreadyExistsException;
import com.intesi.usermanagement.exception.UserNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException ex) {
        log.warn("Utente non trovato: {}", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ProblemDetail handleUserAlreadyExists(UserAlreadyExistsException ex) {
        log.warn("Conflitto utente: {}", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(RoleNotFoundException.class)
    public ProblemDetail handleRoleNotFound(RoleNotFoundException ex) {
        log.warn("Ruolo non trovato: {}", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        log.warn("Errore di validazione: {}", errors);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Errore di validazione");
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        // TOCTOU: race condition tra il check di unicità in UserService e il salvataggio — il DB rileva il conflitto
        log.warn("Violazione di integrità dati: {}", ex.getMessage());
        String detail = resolveIntegrityDetail(ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, detail);
    }

    private String resolveIntegrityDetail(String message) {
        if (message == null) return "Violazione di un vincolo di unicità";
        String lower = message.toLowerCase();
        if (lower.contains("username"))      return "Username già in uso";
        if (lower.contains("email"))         return "Email già in uso";
        if (lower.contains("codice_fiscale")) return "Codice fiscale già in uso";
        return "Violazione di un vincolo di unicità";
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) throws Exception {
        // AccessDeniedException deve essere rilanciata: Spring Security la intercetta per produrre il 403
        if (ex instanceof AccessDeniedException) throw ex;
        log.error("Eccezione non gestita", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Errore interno del server");
    }
}
