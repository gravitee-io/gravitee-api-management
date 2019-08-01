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
package io.gravitee.definition.jackson.services.discovery;

import io.gravitee.definition.jackson.AbstractTest;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.services.discovery.EndpointDiscoveryService;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EndpointDiscoveryServiceSerializerTest extends AbstractTest {

    @Test
    public void definition_withoutEndpointDiscovery() throws Exception {
        String definition = "/io/gravitee/definition/jackson/services/discovery/api-withoutservice.json";
        String expectedDefinition = "/io/gravitee/definition/jackson/services/discovery/api-withoutservice-expected.json";
        Api api = load(definition, Api.class);

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        Assert.assertNotNull(generatedJsonDefinition);

        String expected = IOUtils.toString(read(expectedDefinition));
        JSONAssert.assertEquals(expected, generatedJsonDefinition, false);

        EndpointDiscoveryService endpointDiscoveryService = api.getService(EndpointDiscoveryService.class);
        Assert.assertNull(endpointDiscoveryService);
    }

    @Test
    @Ignore("Service discovery service has been moved from API to group")
    public void definition_withEndpointDiscovery_consul() throws Exception {
        String definition = "/io/gravitee/definition/jackson/services/discovery/api-withservice-consul.json";
        String expectedDefinition = "/io/gravitee/definition/jackson/services/discovery/api-withservice-consul-expected.json";

        Api api = load(definition, Api.class);

        String generatedJsonDefinition = objectMapper().writeValueAsString(api);
        Assert.assertNotNull(generatedJsonDefinition);

        String expected = IOUtils.toString(read(expectedDefinition));
        JSONAssert.assertEquals(expected, generatedJsonDefinition, false);
    }
}
