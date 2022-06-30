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
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.gravitee.definition.jackson.AbstractTest;
import io.gravitee.definition.model.services.healthcheck.HealthCheckService;
import io.gravitee.definition.model.services.healthcheck.HealthCheckStep;
import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
class HealthCheckDeserializerTest extends AbstractTest {

    @Test
    void testHealthCheckDeserializer() throws IOException {
        HealthCheckService healthCheck = load(
            "/io/gravitee/definition/jackson/services/healthcheck/api-healthcheck.json",
            HealthCheckService.class
        );
        assertNotNull(healthCheck);
        assertEquals("*/1 * * * * *", healthCheck.getSchedule());
        assertNotNull(healthCheck.getSteps());
        assertEquals(1, healthCheck.getSteps().size());
        HealthCheckStep step = healthCheck.getSteps().get(0);
        assertNotNull(step.getResponse());
        assertNotNull(step.getRequest());
        assertNotNull(step.getResponse().getAssertions());
        String assertion = step.getResponse().getAssertions().get(0);
        String path = step.getRequest().getPath();
        assertEquals("#response.status == 200", assertion);
        assertEquals("/_health", path);
    }

    @Test
    void testHealthCheckDeserializer_compatMode() throws IOException {
        HealthCheckService healthCheck = load(
            "/io/gravitee/definition/jackson/services/healthcheck/api-healthcheck-compat.json",
            HealthCheckService.class
        );
        assertNotNull(healthCheck);
        assertEquals("*/1 * * * * *", healthCheck.getSchedule());
        assertNotNull(healthCheck.getSteps());
        assertEquals(1, healthCheck.getSteps().size());
        HealthCheckStep step = healthCheck.getSteps().get(0);
        assertNotNull(step.getResponse());
        assertNotNull(step.getRequest());
        assertNotNull(step.getResponse().getAssertions());
        String assertion = step.getResponse().getAssertions().get(0);
        String path = step.getRequest().getPath();
        assertEquals("#response.status == 202", assertion);
        assertEquals("/_health", path);
    }
}
