/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
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
import io.gravitee.rest.api.service.exceptions.AbstractValidationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
class ValidationExceptionMapperTest {

    private ValidationExceptionMapper cut;

    @BeforeEach
    void setUp() {
        cut = new ValidationExceptionMapper();
    }

    @Test
    void shouldMapExceptionToResponse() {
        final FakeValidationException exception = new FakeValidationException();
        try (Response response = cut.toResponse(exception)) {
            assertThat(response.getStatus()).isEqualTo(exception.getHttpStatusCode());
            assertThat(response.getMediaType()).isEqualTo(MediaType.APPLICATION_JSON_TYPE);
            assertThat(response.getEntity())
                .isInstanceOf(Error.class)
                .satisfies(errorObject -> {
                    Error error = ((Error) errorObject);
                    assertThat(error.getHttpStatus()).isEqualTo(exception.getHttpStatusCode());
                    assertThat(error.getMessage()).isEqualTo(exception.getMessage());
                    assertThat(error.getDetails())
                        .hasSize(2)
                        .satisfies(listErrorDetails -> {
                            final ErrorDetailsInner firstDetail = listErrorDetails
                                .stream()
                                .filter(detail -> "key1".equals(detail.getInvalidValue()))
                                .findFirst()
                                .get();
                            assertThat(firstDetail.getInvalidValue()).isEqualTo("key1");
                            assertThat(firstDetail.getMessage()).isEqualTo("fake detail message");
                            assertThat(firstDetail.getLocation()).isEqualTo("value1");

                            final ErrorDetailsInner secondDetail = listErrorDetails
                                .stream()
                                .filter(detail -> "key2".equals(detail.getInvalidValue()))
                                .findFirst()
                                .get();
                            assertThat(secondDetail.getInvalidValue()).isEqualTo("key2");
                            assertThat(secondDetail.getMessage()).isEqualTo("fake detail message");
                            assertThat(secondDetail.getLocation()).isEqualTo("value2");
                        });
                });
        }
    }

    static class FakeValidationException extends AbstractValidationException {

        @Override
        public String getMessage() {
            return "fake exception";
        }

        @Override
        public String getDetailMessage() {
            return "fake detail message";
        }

        @Override
        public String getTechnicalCode() {
            return null;
        }

        @Override
        public Map<String, String> getParameters() {
            return null;
        }

        @Override
        public Map<String, String> getConstraints() {
            return Map.of("key1", "value1", "key2", "value2");
        }
    }
}
