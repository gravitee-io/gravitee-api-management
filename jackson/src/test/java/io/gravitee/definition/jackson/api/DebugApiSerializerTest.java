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
import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class DebugApiSerializerTest extends AbstractTest {

    @Test
    public void debugApi_withDebugSteps() throws Exception {
        DebugApi debugApi = load("/io/gravitee/definition/jackson/debug/debug-api-with-debug-steps.json", DebugApi.class);

        String expectedDefinition = "/io/gravitee/definition/jackson/debug/debug-api-with-debug-steps-expected.json";

        String generatedJsonDefinition = objectMapper().writeValueAsString(debugApi);
        String expectedGeneratedJsonDefinition = IOUtils.toString(read(expectedDefinition));

        assertNotNull(generatedJsonDefinition);

        assertEquals(
            objectMapper().readTree(expectedGeneratedJsonDefinition.getBytes()),
            objectMapper().readTree(generatedJsonDefinition.getBytes())
        );
    }

    @Test
    public void debugApi_withRequestResponse() throws Exception {
        DebugApi debugApi = load("/io/gravitee/definition/jackson/debug/debug-api-with-response.json", DebugApi.class);

        String expectedDefinition = "/io/gravitee/definition/jackson/debug/debug-api-with-response-expected.json";

        String generatedJsonDefinition = objectMapper().writeValueAsString(debugApi);
        String expectedGeneratedJsonDefinition = IOUtils.toString(read(expectedDefinition));

        assertNotNull(generatedJsonDefinition);

        assertEquals(
            objectMapper().readTree(expectedGeneratedJsonDefinition.getBytes()),
            objectMapper().readTree(generatedJsonDefinition.getBytes())
        );
    }
}
