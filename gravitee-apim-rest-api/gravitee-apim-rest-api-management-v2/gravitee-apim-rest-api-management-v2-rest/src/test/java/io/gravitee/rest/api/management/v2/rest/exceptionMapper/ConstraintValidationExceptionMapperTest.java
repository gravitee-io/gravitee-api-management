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
package io.gravitee.rest.api.management.v2.rest.exceptionMapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.management.v2.rest.model.ErrorDetailsInner;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Set;
import org.hibernate.validator.internal.engine.ConstraintViolationImpl;
import org.hibernate.validator.internal.engine.path.PathImpl;
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
                ConstraintViolationImpl.forReturnValueValidation(
                    "fake message",
                    null,
                    null,
                    "Size must be between 1 and 2147483647",
                    null,
                    null,
                    null,
                    List.of(),
                    PathImpl.createPathFromString("path1"),
                    null,
                    null,
                    null
                ),
                ConstraintViolationImpl.forReturnValueValidation(
                    "fake message",
                    null,
                    null,
                    "This value is not allowed",
                    null,
                    null,
                    null,
                    "InvalidValue",
                    PathImpl.createPathFromString("path2"),
                    null,
                    null,
                    null
                )
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

    static class FakeValidationException extends ConstraintViolationException {

        public FakeValidationException(String message, Set<? extends ConstraintViolation<?>> constraintViolations) {
            super(message, constraintViolations);
        }

        public FakeValidationException(Set<? extends ConstraintViolation<?>> constraintViolations) {
            super(constraintViolations);
        }
    }
}
