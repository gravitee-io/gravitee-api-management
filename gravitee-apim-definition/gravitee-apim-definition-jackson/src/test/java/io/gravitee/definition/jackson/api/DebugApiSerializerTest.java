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
package io.gravitee.definition.jackson.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.definition.jackson.AbstractTest;
import io.gravitee.definition.model.debug.DebugApi;
import java.io.IOException;
import java.util.stream.Stream;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class DebugApiSerializerTest extends AbstractTest {

    private static Stream<Arguments> provideParameters() {
        return Stream.of(
            Arguments.of("/io/gravitee/definition/jackson/debug/debug-api-with-backend-response-expected.json"),
            Arguments.of("/io/gravitee/definition/jackson/debug/debug-api-with-debug-steps-expected.json"),
            Arguments.of("/io/gravitee/definition/jackson/debug/debug-api-with-debug-steps-error-expected.json"),
            Arguments.of("/io/gravitee/definition/jackson/debug/debug-api-with-response-expected.json")
        );
    }

    @ParameterizedTest
    @MethodSource("provideParameters")
    public void shouldWriteValidJson(final String json) throws IOException {
        JsonNode jsonNode = loadJson(json);
        String generatedJsonDefinition = objectMapper().writeValueAsString(jsonNode);
        String expectedGeneratedJsonDefinition = IOUtils.toString(read(json));

        Assertions.assertNotNull(generatedJsonDefinition);
        Assertions.assertEquals(
            objectMapper().readTree(expectedGeneratedJsonDefinition.getBytes()),
            objectMapper().readTree(generatedJsonDefinition.getBytes())
        );
    }
}
