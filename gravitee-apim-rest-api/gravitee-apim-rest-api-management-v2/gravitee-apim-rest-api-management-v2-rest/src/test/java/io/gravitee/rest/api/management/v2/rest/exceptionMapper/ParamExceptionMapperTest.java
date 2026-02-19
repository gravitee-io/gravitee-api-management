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
import jakarta.ws.rs.core.Response;
import java.util.stream.Stream;
import org.glassfish.jersey.server.ParamException;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ParamExceptionMapperTest {

    private final ParamExceptionMapper mapper = new ParamExceptionMapper();

    @ParameterizedTest(name = "Exception [{0}] should map to message [{1}]")
    @MethodSource("paramExceptions")
    void should_return_proper_error_message(ParamException exception, String expectedMessage) {
        try (Response response = mapper.toResponse(exception)) {
            assertThat(((Error) response.getEntity()).getMessage()).isEqualTo(expectedMessage);
        }
    }

    private static Stream<Arguments> paramExceptions() {
        var cause = new RuntimeException("invalid");
        return Stream.of(
            Arguments.of(new ParamException.QueryParamException(cause, "page", null), "Query parameter page does not have a valid format"),
            Arguments.of(new ParamException.PathParamException(cause, "apiId", null), "Path parameter apiId does not have a valid format"),
            Arguments.of(new ParamException.FormParamException(cause, "field", null), "Form value field does not have a valid format"),
            Arguments.of(new ParamException.HeaderParamException(cause, "Accept", null), "Header Accept does not have a valid format"),
            Arguments.of(new ParamException.MatrixParamException(cause, "matrix", null), "Parameter matrix does not have a valid format")
        );
    }
}
