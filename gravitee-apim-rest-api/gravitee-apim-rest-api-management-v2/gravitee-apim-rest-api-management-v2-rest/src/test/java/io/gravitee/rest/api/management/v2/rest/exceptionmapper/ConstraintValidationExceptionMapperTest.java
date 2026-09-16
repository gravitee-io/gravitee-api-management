/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.management.v2.rest.exceptionmapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.management.v2.rest.model.ErrorDetailsInner;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
class ConstraintValidationExceptionMapperTest {

    private ConstraintValidationExceptionMapper cve;

    @BeforeEach
    void setUp() {
        cve = new ConstraintValidationExceptionMapper();
    }

    @Test
    void shouldMapExceptionToResponse() {
        final ConstraintViolationException exception = new FakeValidationException(
            "fake message",
            Set.of(
                violation("path1", "Size must be between 1 and 2147483647", List.of()),
                violation("path2", "This value is not allowed", "InvalidValue")
            )
        );

        try (Response response = cve.toResponse(exception)) {
            assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
            assertThat(response.getMediaType()).isEqualTo(MediaType.APPLICATION_JSON_TYPE);
            assertThat(response.getEntity())
                .isInstanceOf(Error.class)
                .satisfies(errorObject -> {
                    Error error = ((Error) errorObject);
                    assertThat(error.getHttpStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
                    assertThat(error.getMessage()).isEqualTo("Validation error");
                    assertThat(error.getDetails())
                        .hasSize(2)
                        .satisfies(listErrorDetails -> {
                            final ErrorDetailsInner firstDetail = listErrorDetails
                                .stream()
                                .filter(detail -> "path1".equals(detail.getLocation()))
                                .findFirst()
                                .get();
                            assertThat(firstDetail.getInvalidValue()).isEqualTo(JsonNullable.of(List.of()));
                            assertThat(firstDetail.getMessage()).isEqualTo("Size must be between 1 and 2147483647");
                            assertThat(firstDetail.getLocation()).isEqualTo("path1");

                            final ErrorDetailsInner secondDetail = listErrorDetails
                                .stream()
                                .filter(detail -> "path2".equals(detail.getLocation()))
                                .findFirst()
                                .get();
                            assertThat(secondDetail.getInvalidValue()).isEqualTo(JsonNullable.of("InvalidValue"));
                            assertThat(secondDetail.getMessage()).isEqualTo("This value is not allowed");
                            assertThat(secondDetail.getLocation()).isEqualTo("path2");
                        });
                });
        }
    }

    @SuppressWarnings("unchecked")
    private static ConstraintViolation<Object> violation(String propertyPath, String message, Object invalidValue) {
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        when(violation.getPropertyPath()).thenReturn(path(propertyPath));
        when(violation.getMessage()).thenReturn(message);
        when(violation.getInvalidValue()).thenReturn(invalidValue);
        return violation;
    }

    private static Path path(String value) {
        return new Path() {
            @Override
            public Iterator<Node> iterator() {
                return Collections.emptyIterator();
            }

            @Override
            public String toString() {
                return value;
            }
        };
    }

    static class FakeValidationException extends ConstraintViolationException {

        public FakeValidationException(String message, Set<? extends ConstraintViolation<?>> constraintViolations) {
            super(message, constraintViolations);
        }

        public FakeValidationException(Set<? extends ConstraintViolation<?>> constraintViolations) {
            super(constraintViolations);
        }
    }
}
