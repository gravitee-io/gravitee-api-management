package io.gravitee.gamma.module.authz.rest;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gamma.module.authz.entityimport.service.AuthzValidationException;
import io.gravitee.gamma.module.authz.entityimport.service.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResponseErrorsTest {

    @Test
    void unhandled_exception_returns_500_with_generic_message_not_cause() {
        Response r = ResponseErrors.call(() -> {
            throw new RuntimeException("Bearer eyJxxx.secret-token.yyy leaked in upstream URL");
        });

        assertThat(r.getStatus()).isEqualTo(500);
        String body = r.getEntity().toString();
        assertThat(body).doesNotContain("secret-token", "Bearer", "eyJxxx").contains("Internal error");
    }

    @Test
    void mongo_style_exception_message_not_leaked_to_client() {
        Response r = ResponseErrors.call(() -> {
            throw new IllegalStateException(
                "com.mongodb.MongoSocketReadException: Exception receiving message from server idp-secrets-db:27017"
            );
        });

        assertThat(r.getStatus()).isEqualTo(500);
        assertThat(r.getEntity().toString()).doesNotContain("MongoSocket", "idp-secrets-db");
    }

    @Test
    void validation_exception_still_returns_400_with_message() {
        Response r = ResponseErrors.call(() -> {
            throw new AuthzValidationException("name already exists", List.of("name: duplicate"));
        });

        assertThat(r.getStatus()).isEqualTo(400);
        assertThat(r.getEntity().toString()).contains("name already exists");
    }

    @Test
    void not_found_exception_returns_404_with_message() {
        Response r = ResponseErrors.call(() -> {
            throw new NotFoundException("Connector not found: abc");
        });

        assertThat(r.getStatus()).isEqualTo(404);
        assertThat(r.getEntity().toString()).contains("Connector not found: abc");
    }

    @Test
    void web_application_exception_is_rethrown_untouched() {
        WebApplicationException original = new WebApplicationException("upstream said no", 502);

        org.assertj.core.api.ThrowableAssert.ThrowingCallable invocation = () ->
            ResponseErrors.call(() -> {
                throw original;
            });

        assertThat(catchThrowable(invocation)).isSameAs(original);
    }

    private static Throwable catchThrowable(org.assertj.core.api.ThrowableAssert.ThrowingCallable c) {
        try {
            c.call();
            return null;
        } catch (Throwable t) {
            return t;
        }
    }
}
