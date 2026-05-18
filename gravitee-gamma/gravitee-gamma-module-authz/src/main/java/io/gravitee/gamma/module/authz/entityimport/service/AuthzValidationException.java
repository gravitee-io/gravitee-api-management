package io.gravitee.gamma.module.authz.entityimport.service;

import java.util.List;

public class AuthzValidationException extends RuntimeException {

    private final List<String> errors;

    public AuthzValidationException(String message, List<String> errors) {
        super(message);
        this.errors = List.copyOf(errors);
    }

    public AuthzValidationException(String message) {
        this(message, List.of());
    }

    public List<String> getErrors() {
        return errors;
    }
}
