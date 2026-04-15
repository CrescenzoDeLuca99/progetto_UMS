package com.intesi.usermanagement.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = CodiceFiscaleValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCodiceFiscale {

    String message() default "Codice fiscale non valido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
