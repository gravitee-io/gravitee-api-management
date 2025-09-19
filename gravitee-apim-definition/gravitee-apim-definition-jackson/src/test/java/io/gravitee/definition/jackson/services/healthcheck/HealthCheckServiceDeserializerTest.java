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
package io.gravitee.definition.jackson.services.healthcheck;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonMappingException;
import io.gravitee.definition.jackson.AbstractTest;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.services.healthcheck.HealthCheckService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HealthCheckServiceDeserializerTest extends AbstractTest {

    @Test
    public void healthcheck_withoutservice() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/services/healthcheck/api-withoutservice-healthcheck.json", Api.class);
        HealthCheckService healthCheckService = api.getServices().get(HealthCheckService.class);
        Assertions.assertNull(healthCheckService);
        /*
        This test is to ensure that we remove the boolean "healthcheck" in the root of the definition as it was done before the SME
         */
        Assertions.assertFalse(
            api
                .getProxy()
                .getGroups()
                .iterator()
                .next()
                .getEndpoints()
                .iterator()
                .next()
                .getConfiguration()
                .contains("\"healthcheck\":true")
        );
    }

    @Test
    public void healthcheck() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/services/healthcheck/api-withservice-healthcheck.json", Api.class);
        HealthCheckService healthCheckService = api.getServices().get(HealthCheckService.class);
        Assertions.assertNotNull(healthCheckService);
        Assertions.assertTrue(healthCheckService.isEnabled());
        Assertions.assertEquals("*/60 * * * * *", healthCheckService.getSchedule());

        // Check step
        Assertions.assertFalse(healthCheckService.getSteps().isEmpty());

        // Check request
        Assertions.assertNotNull(healthCheckService.getSteps().get(0).getRequest());
        Assertions.assertNotNull(healthCheckService.getSteps().get(0).getRequest().getPath());

        // Check expectations
        Assertions.assertNotNull(healthCheckService.getSteps().get(0).getResponse());
    }

    @Test
    public void healthcheck_v2() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/services/healthcheck/api-withservice-healthcheck-v2.json", Api.class);
        HealthCheckService healthCheckService = api.getServices().get(HealthCheckService.class);
        Assertions.assertNotNull(healthCheckService);
        Assertions.assertTrue(healthCheckService.isEnabled());
        Assertions.assertEquals("*/60 * * * * *", healthCheckService.getSchedule());

        // Check step
        Assertions.assertFalse(healthCheckService.getSteps().isEmpty());

        // Check request
        Assertions.assertNotNull(healthCheckService.getSteps().get(0).getRequest());
        Assertions.assertNotNull(healthCheckService.getSteps().get(0).getRequest().getPath());
        Assertions.assertFalse(healthCheckService.getSteps().get(0).getRequest().isFromRoot());

        // Check expectations
        Assertions.assertNotNull(healthCheckService.getSteps().get(0).getResponse());
    }

    @Test
    public void healthcheck_v3() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/services/healthcheck/api-withservice-healthcheck-v3.json", Api.class);
        HealthCheckService healthCheckService = api.getServices().get(HealthCheckService.class);
        Assertions.assertNotNull(healthCheckService);
        Assertions.assertTrue(healthCheckService.isEnabled());
        Assertions.assertEquals("*/60 * * * * *", healthCheckService.getSchedule());

        // Check step
        Assertions.assertFalse(healthCheckService.getSteps().isEmpty());

        // Check request
        Assertions.assertNotNull(healthCheckService.getSteps().get(0).getRequest());
        Assertions.assertNotNull(healthCheckService.getSteps().get(0).getRequest().getPath());
        Assertions.assertFalse(healthCheckService.getSteps().get(0).getRequest().isFromRoot());

        // Check expectations
        Assertions.assertNotNull(healthCheckService.getSteps().get(0).getResponse());
    }

    @Test
    public void healthcheck_badUnit() throws Exception {
        assertThrows(JsonMappingException.class, () ->
            load("/io/gravitee/definition/jackson/services/healthcheck/api-withservice-healthcheck-badUnit.json", Api.class)
        );
    }

    @Test
    public void healthcheck_unitInLowerCase() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/services/healthcheck/api-withservice-healthcheck-unitInLowerCase.json", Api.class);
        HealthCheckService healthCheckService = api.getServices().get(HealthCheckService.class);
        Assertions.assertNotNull(healthCheckService);
        Assertions.assertTrue(healthCheckService.isEnabled());
        Assertions.assertEquals("*/60 * * * * *", healthCheckService.getSchedule());
    }

    @Test
    public void healthcheck_noExpectation() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/services/healthcheck/api-withservice-healthcheck-noExpectation.json", Api.class);
        HealthCheckService healthCheckService = api.getServices().get(HealthCheckService.class);

        // Check step
        Assertions.assertFalse(healthCheckService.getSteps().isEmpty());

        Assertions.assertFalse(healthCheckService.getSteps().get(0).getResponse().getAssertions().isEmpty());
    }

    @Test
    public void healthcheck_v2_fromRoot() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/services/healthcheck/api-withservice-healthcheck-v2-fromroot.json", Api.class);
        HealthCheckService healthCheckService = api.getServices().get(HealthCheckService.class);
        Assertions.assertNotNull(healthCheckService);

        // Check step
        Assertions.assertFalse(healthCheckService.getSteps().isEmpty());

        // Check request
        Assertions.assertTrue(healthCheckService.getSteps().get(0).getRequest().isFromRoot());
    }

    @Test
    public void healthcheck_disabled() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/services/healthcheck/api-withservice-healthcheck-disabled.json", Api.class);
        HealthCheckService healthCheckService = api.getServices().get(HealthCheckService.class);
        Assertions.assertNotNull(healthCheckService);
        Assertions.assertNull(healthCheckService.getSchedule());
    }
}
