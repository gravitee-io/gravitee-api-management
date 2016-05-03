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

import io.gravitee.definition.jackson.AbstractTest;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.services.healthcheck.HealthCheck;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public class HealthCheckDeserializerTest extends AbstractTest {

    @Test
    public void healthcheck() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-withservice-healthcheck.json", Api.class);
        HealthCheck healthCheckService = api.getServices().get(HealthCheck.class);
        Assert.assertNotNull(healthCheckService);
        Assert.assertTrue(healthCheckService.isEnabled());
        Assert.assertEquals(60, healthCheckService.getInterval());
        Assert.assertEquals(TimeUnit.SECONDS, healthCheckService.getUnit());

        // Check request
        Assert.assertNotNull(healthCheckService.getRequest());
        Assert.assertNotNull(healthCheckService.getRequest().getUri());

        // Check expectations
        Assert.assertNotNull(healthCheckService.getExpectation());
    }

    @Test(expected = IllegalArgumentException.class)
    public void healthcheck_badUnit() throws Exception {
        load("/io/gravitee/definition/jackson/api-withservice-healthcheck-badUnit.json", Api.class);
    }

    @Test
    public void healthcheck_unitInLowerCase() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/api-withservice-healthcheck-unitInLowerCase.json", Api.class);
        HealthCheck healthCheckService = api.getServices().get(HealthCheck.class);
        Assert.assertNotNull(healthCheckService);
        Assert.assertFalse(healthCheckService.isEnabled());
        Assert.assertEquals(60, healthCheckService.getInterval());
        Assert.assertEquals(TimeUnit.SECONDS, healthCheckService.getUnit());
    }

    @Test//(expected = JsonMappingException.class)
    public void healthcheck_noExpectation() throws Exception {
        load("/io/gravitee/definition/jackson/api-withservice-healthcheck-noExpectation.json", Api.class);
    }
}
