package io.gravitee.gamma.module.authz.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gamma.module.authz.entityimport.model.ValidationErrorResponse;
import io.gravitee.gamma.module.authz.entityimport.service.AuthzValidationException;
import io.gravitee.gamma.module.authz.entityimport.service.NotFoundException;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.concurrent.Callable;

public final class ResponseErrors {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ResponseErrors() {}

    public static Response call(Callable<Response> c) {
        try {
            return c.call();
        } catch (AuthzValidationException e) {
            return jsonResponse(Response.Status.BAD_REQUEST,
                ValidationErrorResponse.of(e.getMessage(), e.getErrors()));
        } catch (ConstraintViolationException e) {
            List<String> messages = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .toList();
            return jsonResponse(Response.Status.BAD_REQUEST,
                ValidationErrorResponse.of("Validation failed", messages));
        } catch (NotFoundException e) {
            return jsonResponse(Response.Status.NOT_FOUND,
                new ErrorBody(e.getMessage(), 404));
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }

    private static Response jsonResponse(Response.Status status, Object body) {
        try {
            return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(MAPPER.writeValueAsString(body))
                .build();
        } catch (JsonProcessingException ex) {
            return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity("{\"message\":\"Error serialization failed\"}")
                .build();
        }
    }

    public record ErrorBody(String message, int status) {}
}
