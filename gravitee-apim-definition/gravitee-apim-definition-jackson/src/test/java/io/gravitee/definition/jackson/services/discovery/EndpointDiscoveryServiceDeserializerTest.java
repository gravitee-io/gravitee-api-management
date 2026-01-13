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
package io.gravitee.definition.jackson.services.discovery;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.definition.jackson.AbstractTest;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.services.discovery.EndpointDiscoveryService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EndpointDiscoveryServiceDeserializerTest extends AbstractTest {

    private static final String CONSUL_SERVICE_DISCOVERY_ID = "consul-service-discovery";
    private static final String KUBERNETES_SERVICE_DISCOVERY_ID = "kubernetes-service-discovery";

    @Test
    public void definition_withoutEndpointDiscovery() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/services/discovery/api-withoutservice.json", Api.class);

        EndpointDiscoveryService endpointDiscoveryService = api.getService(EndpointDiscoveryService.class);
        Assertions.assertNull(endpointDiscoveryService);
    }

    @Test
    public void definition_withEndpointDiscovery_consul() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/services/discovery/api-withservice-consul.json", Api.class);

        EndpointDiscoveryService endpointDiscoveryService = getDiscoveryService(api);
        Assertions.assertNotNull(endpointDiscoveryService);
        Assertions.assertNotNull(endpointDiscoveryService.getConfiguration());

        Assertions.assertEquals(CONSUL_SERVICE_DISCOVERY_ID, endpointDiscoveryService.getProvider());
        Assertions.assertNotNull(endpointDiscoveryService.getConfiguration());

        JsonNode configNode = objectMapper().readTree(endpointDiscoveryService.getConfiguration());
        Assertions.assertEquals("my-service", configNode.path("service").asText());
        Assertions.assertEquals("acl", configNode.path("acl").asText());
        Assertions.assertEquals("dc", configNode.path("dc").asText());
    }

    @Test
    public void definition_withEndpointDiscovery_kubernetes() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/services/discovery/api-withservice-kubernetes.json", Api.class);

        EndpointDiscoveryService endpointDiscoveryService = getDiscoveryService(api);
        Assertions.assertNotNull(endpointDiscoveryService);
        Assertions.assertNotNull(endpointDiscoveryService.getConfiguration());

        Assertions.assertEquals(KUBERNETES_SERVICE_DISCOVERY_ID, endpointDiscoveryService.getProvider());

        JsonNode configNode = objectMapper().readTree(endpointDiscoveryService.getConfiguration());
        Assertions.assertEquals("default", configNode.path("namespace").asText());
        Assertions.assertEquals("my-service", configNode.path("service").asText());
        Assertions.assertEquals(8080, configNode.path("port").asInt());
        Assertions.assertEquals("https", configNode.path("scheme").asText());
        Assertions.assertEquals("/api", configNode.path("path").asText());
    }

    private EndpointDiscoveryService getDiscoveryService(Api api) {
        return api.getProxy().getGroups().iterator().next().getServices().get(EndpointDiscoveryService.class);
    }
}
