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

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.definition.jackson.AbstractTest;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.services.discovery.EndpointDiscoveryService;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EndpointDiscoveryServiceDeserializerTest extends AbstractTest {

    @Test
    public void definition_withoutEndpointDiscovery() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/services/discovery/api-withoutservice.json", Api.class);

        EndpointDiscoveryService endpointDiscoveryService = api.getService(EndpointDiscoveryService.class);
        Assert.assertNull(endpointDiscoveryService);
    }

    @Test
    public void definition_withEndpointDiscovery_consul() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/services/discovery/api-withservice-consul.json", Api.class);

        EndpointDiscoveryService endpointDiscoveryService = api
            .getProxy()
            .getGroups()
            .iterator()
            .next()
            .getServices()
            .get(EndpointDiscoveryService.class);
        Assert.assertNotNull(endpointDiscoveryService);
        Assert.assertNotNull(endpointDiscoveryService.getConfiguration());

        Assert.assertEquals("consul-service-discovery", endpointDiscoveryService.getProvider());
        Assert.assertNotNull(endpointDiscoveryService.getConfiguration());

        JsonNode configNode = objectMapper().readTree(endpointDiscoveryService.getConfiguration());
        Assert.assertEquals("my-service", configNode.path("service").asText());
        Assert.assertEquals("acl", configNode.path("acl").asText());
        Assert.assertEquals("dc", configNode.path("dc").asText());
    }
}
