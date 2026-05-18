package io.gravitee.gamma.module.authz.entityimport.service;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
