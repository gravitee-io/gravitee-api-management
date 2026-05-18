package io.gravitee.gamma.module.authz.entityimport.model;

import java.util.List;

public record ValidationErrorResponse(String message, int status, List<String> errors) {
    public static ValidationErrorResponse of(String message, List<String> errors) {
        return new ValidationErrorResponse(message, 400, errors);
    }

    public static ValidationErrorResponse of(String message) {
        return new ValidationErrorResponse(message, 400, List.of());
    }
}
