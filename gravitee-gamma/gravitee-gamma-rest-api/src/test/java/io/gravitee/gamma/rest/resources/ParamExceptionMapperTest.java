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
package io.gravitee.gamma.rest.resources;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.management.rest.model.ErrorEntity;
import jakarta.ws.rs.core.Response;
import java.util.stream.Stream;
import org.glassfish.jersey.server.ParamException;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ParamExceptionMapperTest {

    private final ParamExceptionMapper mapper = new ParamExceptionMapper();

    @ParameterizedTest(name = "{1}")
    @MethodSource("parameters")
    void should_name_the_refused_parameter_and_where_it_came_from(ParamException exception, String expectedMessage) {
        try (Response response = mapper.toResponse(exception)) {
            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
            assertThat(((ErrorEntity) response.getEntity()).getMessage()).isEqualTo(expectedMessage);
        }
    }

    private static Stream<Arguments> parameters() {
        var cause = new NumberFormatException("For input string: \"abc\"");
        return Stream.of(
            Arguments.of(new ParamException.QueryParamException(cause, "page", null), "Query parameter page does not have a valid format"),
            // A module plugin declares UUID ids, so this one is reached in production, not only in theory.
            Arguments.of(new ParamException.PathParamException(cause, "id", null), "Path parameter id does not have a valid format"),
            Arguments.of(
                new ParamException.HeaderParamException(cause, "X-Gravitee", null),
                "Parameter X-Gravitee does not have a valid format"
            )
        );
    }

    /** The submitted value belongs to the cause, which the answer must not carry back to the caller. */
    @ParameterizedTest(name = "{1}")
    @MethodSource("parameters")
    void should_not_echo_the_submitted_value(ParamException exception, String ignored) {
        try (Response response = mapper.toResponse(exception)) {
            assertThat(((ErrorEntity) response.getEntity()).getMessage()).doesNotContain("abc");
        }
    }
}
