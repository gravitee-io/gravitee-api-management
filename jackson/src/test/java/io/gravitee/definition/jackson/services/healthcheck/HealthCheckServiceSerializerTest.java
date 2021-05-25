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
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HealthCheckServiceSerializerTest extends AbstractTest {

    @Test
    public void definition_withHealtcheck() throws Exception {
        String oldDefinition = "/io/gravitee/definition/jackson/services/healtcheck/api-withservice-healthcheck.json";
        String expectedDefinition = "/io/gravitee/definition/jackson/services/healtcheck/api-withservice-healthcheck-v3-expected.json";
        Api api = load(oldDefinition, Api.class);

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        Assert.assertNotNull(generatedJsonDefinition);

        String expected = IOUtils.toString(read(expectedDefinition));
        JSONAssert.assertEquals(expected, generatedJsonDefinition, false);
    }

    @Test
    public void definition_withHealtcheck_v2() throws Exception {
        String definition = "/io/gravitee/definition/jackson/services/healtcheck/api-withservice-healthcheck-v2.json";
        String expectedDefinition = "/io/gravitee/definition/jackson/services/healtcheck/api-withservice-healthcheck-v3-expected.json";
        Api api = load(definition, Api.class);

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        Assert.assertNotNull(generatedJsonDefinition);

        String expected = IOUtils.toString(read(expectedDefinition));
        JSONAssert.assertEquals(expected, generatedJsonDefinition, false);
    }

    @Test
    public void definition_withHealtcheck_v3() throws Exception {
        String definition = "/io/gravitee/definition/jackson/services/healtcheck/api-withservice-healthcheck-v3.json";
        String expectedDefinition = "/io/gravitee/definition/jackson/services/healtcheck/api-withservice-healthcheck-v3-expected.json";
        Api api = load(definition, Api.class);

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        Assert.assertNotNull(generatedJsonDefinition);

        String expected = IOUtils.toString(read(expectedDefinition));
        JSONAssert.assertEquals(expected, generatedJsonDefinition, false);
    }

    @Test
    public void definition_withHealtcheck_v2_fromRoot() throws Exception {
        String definition = "/io/gravitee/definition/jackson/services/healtcheck/api-withservice-healthcheck-v2-fromroot.json";
        String expectedDefinition =
            "/io/gravitee/definition/jackson/services/healtcheck/api-withservice-healthcheck-v3-fromroot-expected.json";
        Api api = load(definition, Api.class);

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        Assert.assertNotNull(generatedJsonDefinition);

        String expected = IOUtils.toString(read(expectedDefinition));
        JSONAssert.assertEquals(expected, generatedJsonDefinition, false);
    }

    @Test
    public void definition_withHealtcheck_v3_fromRoot() throws Exception {
        String definition = "/io/gravitee/definition/jackson/services/healtcheck/api-withservice-healthcheck-v3-fromroot.json";
        String expectedDefinition =
            "/io/gravitee/definition/jackson/services/healtcheck/api-withservice-healthcheck-v3-fromroot-expected.json";
        Api api = load(definition, Api.class);

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        Assert.assertNotNull(generatedJsonDefinition);

        String expected = IOUtils.toString(read(expectedDefinition));
        JSONAssert.assertEquals(expected, generatedJsonDefinition, false);
    }

    @Test
    public void definition_withHealtcheck_disabled() throws Exception {
        String definition = "/io/gravitee/definition/jackson/services/healtcheck/api-withservice-healthcheck-disabled.json";
        String expectedDefinition = "/io/gravitee/definition/jackson/services/healtcheck/api-withservice-healthcheck-expected.json";
        Api api = load(definition, Api.class);

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        Assert.assertNotNull(generatedJsonDefinition);

        String expected = IOUtils.toString(read(expectedDefinition));
        JSONAssert.assertEquals(expected, generatedJsonDefinition, false);
    }
}
