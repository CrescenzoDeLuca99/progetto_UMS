package com.intesi.usermanagement.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class CodiceFiscaleValidator implements ConstraintValidator<ValidCodiceFiscale, String> {

    private static final Pattern CF_PATTERN = Pattern.compile(
            "^[A-Z]{6}[0-9LMNPQRSTUV]{2}[ABCDEHLMPRST][0-9LMNPQRSTUV]{2}[A-Z][0-9LMNPQRSTUV]{3}[A-Z]$",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // null è valido: la presenza è demandata a @NotNull, per composizione delle constraint
        if (value == null) {
            return true;
        }
        return CF_PATTERN.matcher(value.trim()).matches();
    }
}
