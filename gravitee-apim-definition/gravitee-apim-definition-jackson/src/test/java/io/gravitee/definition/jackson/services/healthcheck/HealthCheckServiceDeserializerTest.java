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

import com.fasterxml.jackson.databind.JsonMappingException;
import io.gravitee.definition.jackson.AbstractTest;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.services.healthcheck.HealthCheckService;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HealthCheckServiceDeserializerTest extends AbstractTest {

    @Test
    public void healthcheck() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/services/healtcheck/api-withservice-healthcheck.json", Api.class);
        HealthCheckService healthCheckService = api.getServices().get(HealthCheckService.class);
        Assert.assertNotNull(healthCheckService);
        Assert.assertTrue(healthCheckService.isEnabled());
        Assert.assertEquals(60, healthCheckService.getTrigger().getRate());
        Assert.assertEquals(TimeUnit.SECONDS, healthCheckService.getTrigger().getUnit());

        // Check step
        Assert.assertFalse(healthCheckService.getSteps().isEmpty());

        // Check request
        Assert.assertNotNull(healthCheckService.getSteps().get(0).getRequest());
        Assert.assertNotNull(healthCheckService.getSteps().get(0).getRequest().getPath());

        // Check expectations
        Assert.assertNotNull(healthCheckService.getSteps().get(0).getResponse());
    }

    @Test
    public void healthcheck_v2() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/services/healtcheck/api-withservice-healthcheck-v2.json", Api.class);
        HealthCheckService healthCheckService = api.getServices().get(HealthCheckService.class);
        Assert.assertNotNull(healthCheckService);
        Assert.assertTrue(healthCheckService.isEnabled());
        Assert.assertEquals(60, healthCheckService.getTrigger().getRate());
        Assert.assertEquals(TimeUnit.SECONDS, healthCheckService.getTrigger().getUnit());

        // Check step
        Assert.assertFalse(healthCheckService.getSteps().isEmpty());

        // Check request
        Assert.assertNotNull(healthCheckService.getSteps().get(0).getRequest());
        Assert.assertNotNull(healthCheckService.getSteps().get(0).getRequest().getPath());
        Assert.assertFalse(healthCheckService.getSteps().get(0).getRequest().isFromRoot());

        // Check expectations
        Assert.assertNotNull(healthCheckService.getSteps().get(0).getResponse());
    }

    @Test(expected = JsonMappingException.class)
    public void healthcheck_badUnit() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/services/healtcheck/api-withservice-healthcheck-badUnit.json", Api.class);

        Assert.assertNotNull(api);
    }

    @Test
    public void healthcheck_unitInLowerCase() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/services/healtcheck/api-withservice-healthcheck-unitInLowerCase.json", Api.class);
        HealthCheckService healthCheckService = api.getServices().get(HealthCheckService.class);
        Assert.assertNotNull(healthCheckService);
        Assert.assertTrue(healthCheckService.isEnabled());
        Assert.assertEquals(60, healthCheckService.getTrigger().getRate());
        Assert.assertEquals(TimeUnit.SECONDS, healthCheckService.getTrigger().getUnit());
    }

    @Test
    public void healthcheck_noExpectation() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/services/healtcheck/api-withservice-healthcheck-noExpectation.json", Api.class);
        HealthCheckService healthCheckService = api.getServices().get(HealthCheckService.class);

        // Check step
        Assert.assertFalse(healthCheckService.getSteps().isEmpty());

        Assert.assertFalse(healthCheckService.getSteps().get(0).getResponse().getAssertions().isEmpty());
    }

    @Test
    public void healthcheck_v2_fromRoot() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/services/healtcheck/api-withservice-healthcheck-v2-fromroot.json", Api.class);
        HealthCheckService healthCheckService = api.getServices().get(HealthCheckService.class);
        Assert.assertNotNull(healthCheckService);

        // Check step
        Assert.assertFalse(healthCheckService.getSteps().isEmpty());

        // Check request
        Assert.assertTrue(healthCheckService.getSteps().get(0).getRequest().isFromRoot());
    }
}
