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

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JsonMappingExceptionMapperTest {

    private JsonMappingExceptionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new JsonMappingExceptionMapper();
    }

    @Test
    void should_sanitize_class_name_from_value_instantiation_exception() {
        var cause = new IllegalArgumentException("Unexpected value 'UNSUPPORTED_METRIC'");
        var exception = ValueInstantiationException.from(
            null,
            "Cannot construct instance of io.gravitee.rest.api.management.v2.rest.model.analytics.engine.MetricName, problem: Unexpected value 'UNSUPPORTED_METRIC'",
            (JavaType) null,
            cause
        );

        try (Response response = mapper.toResponse(exception)) {
            assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
            assertThat(response.getMediaType()).isEqualTo(MediaType.APPLICATION_JSON_TYPE);
            assertThat(response.getEntity()).isInstanceOf(Error.class);

            Error error = (Error) response.getEntity();
            assertThat(error.getHttpStatus()).isEqualTo(400);
            assertThat(error.getMessage()).isEqualTo("Unexpected value 'UNSUPPORTED_METRIC'");
            assertThat(error.getMessage()).doesNotContain("io.gravitee");
            assertThat(error.getTechnicalCode()).isEqualTo("invalidValue");
        }
    }

    @Test
    void should_use_original_message_for_regular_json_mapping_exception() {
        var exception = JsonMappingException.from(
            (com.fasterxml.jackson.core.JsonParser) null,
            "Cannot deserialize value of type `String` from Array value"
        );

        try (Response response = mapper.toResponse(exception)) {
            assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());

            Error error = (Error) response.getEntity();
            assertThat(error.getMessage()).isEqualTo("Cannot deserialize value of type `String` from Array value");
            assertThat(error.getTechnicalCode()).isEqualTo("invalidValue");
        }
    }

    @Test
    void should_include_field_location_in_details() {
        var cause = new IllegalArgumentException("Unexpected value 'INVALID'");
        var exception = ValueInstantiationException.from(null, "problem", (JavaType) null, cause);
        exception.prependPath(null, "name");
        exception.prependPath(null, "metrics");

        try (Response response = mapper.toResponse(exception)) {
            Error error = (Error) response.getEntity();
            assertThat(error.getDetails()).hasSize(1);
            assertThat(error.getDetails().getFirst().getLocation()).isEqualTo("metrics.name");
            assertThat(error.getDetails().getFirst().getMessage()).isEqualTo("Unexpected value 'INVALID'");
        }
    }

    @Test
    void should_format_array_index_in_field_location() {
        var cause = new IllegalArgumentException("Unexpected value 'INVALID'");
        var exception = ValueInstantiationException.from(null, "problem", (JavaType) null, cause);
        exception.prependPath(null, "name");
        exception.prependPath(new Object(), 0);
        exception.prependPath(null, "metrics");

        try (Response response = mapper.toResponse(exception)) {
            Error error = (Error) response.getEntity();
            assertThat(error.getDetails()).hasSize(1);
            assertThat(error.getDetails().getFirst().getLocation()).isEqualTo("metrics[0].name");
        }
    }

    @Test
    void should_not_include_details_when_no_path() {
        var exception = JsonMappingException.from((com.fasterxml.jackson.core.JsonParser) null, "Some error");

        try (Response response = mapper.toResponse(exception)) {
            Error error = (Error) response.getEntity();
            assertThat(error.getDetails()).isNullOrEmpty();
        }
    }
}
