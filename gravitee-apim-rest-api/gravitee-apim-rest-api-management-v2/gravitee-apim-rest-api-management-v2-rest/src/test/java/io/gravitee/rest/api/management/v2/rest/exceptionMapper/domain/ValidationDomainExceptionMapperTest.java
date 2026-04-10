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
package io.gravitee.rest.api.management.v2.rest.exceptionMapper.domain;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.analytics_engine.exception.InvalidQueryException;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ValidationDomainExceptionMapperTest {

    private ValidationDomainExceptionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ValidationDomainExceptionMapper();
    }

    @Test
    void should_include_technical_code_when_present() {
        var exception = InvalidQueryException.forUnknownAPIType("INVALID_TYPE");

        try (Response response = mapper.toResponse(exception)) {
            assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
            assertThat(response.getMediaType()).isEqualTo(MediaType.APPLICATION_JSON_TYPE);
            assertThat(response.getEntity()).isInstanceOf(Error.class);

            Error error = (Error) response.getEntity();
            assertThat(error.getHttpStatus()).isEqualTo(400);
            assertThat(error.getMessage()).isEqualTo("Unknown API type 'INVALID_TYPE'");
            assertThat(error.getTechnicalCode()).isEqualTo("analytics.filter.invalidValue");
        }
    }

    @Test
    void should_handle_null_technical_code() {
        var exception = new ValidationDomainException("Some validation error");

        try (Response response = mapper.toResponse(exception)) {
            assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());

            Error error = (Error) response.getEntity();
            assertThat(error.getHttpStatus()).isEqualTo(400);
            assertThat(error.getMessage()).isEqualTo("Some validation error");
            assertThat(error.getTechnicalCode()).isNull();
        }
    }

    @Test
    void should_include_parameters_when_present() {
        var params = Map.of("field", "apiType");
        var exception = new ValidationDomainException("Invalid value", params, "analytics.filter.invalidValue");

        try (Response response = mapper.toResponse(exception)) {
            Error error = (Error) response.getEntity();
            assertThat(error.getParameters()).containsEntry("field", "apiType");
            assertThat(error.getTechnicalCode()).isEqualTo("analytics.filter.invalidValue");
        }
    }
}
