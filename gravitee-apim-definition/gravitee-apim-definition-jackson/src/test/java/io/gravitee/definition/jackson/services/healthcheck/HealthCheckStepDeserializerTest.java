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
package io.gravitee.definition.jackson.services.healthcheck;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.JsonMappingException;
import io.gravitee.definition.jackson.AbstractTest;
import io.gravitee.definition.model.services.healthcheck.HealthCheckStep;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
class HealthCheckStepDeserializerTest extends AbstractTest {

    @Test
    void testHealthCheckStepDeserializer() throws IOException {
        HealthCheckStep step = load("/io/gravitee/definition/jackson/services/healtcheck/api-healthcheck-step.json", HealthCheckStep.class);
        assertNotNull(step);
        assertEquals("default-step", step.getName());
        assertNotNull(step.getRequest());
        assertEquals("/_health", step.getRequest().getPath());
        assertNotNull(step.getResponse());
        assertEquals(List.of("#response.status == 200"), step.getResponse().getAssertions());
    }

    @Test
    void testHealthCheckStepDeserializer_noRequest() throws IOException {
        assertThrows(
            JsonMappingException.class,
            () -> {
                load("/io/gravitee/definition/jackson/services/healtcheck/api-healthcheck-step-no-request.json", HealthCheckStep.class);
            }
        );
    }

    @Test
    void testHealthCheckStepDeserializer_noResponse() throws IOException {
        HealthCheckStep step = load(
            "/io/gravitee/definition/jackson/services/healtcheck/api-healthcheck-step-no-response.json",
            HealthCheckStep.class
        );
        assertNotNull(step.getResponse());
        assertEquals(List.of("#response.status == 200"), step.getResponse().getAssertions());
    }
}
