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

import com.fasterxml.jackson.databind.JsonMappingException;
import io.gravitee.definition.jackson.AbstractTest;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.services.discovery.EndpointDiscoveryProvider;
import io.gravitee.definition.model.services.discovery.EndpointDiscoveryService;
import io.gravitee.definition.model.services.discovery.consul.ConsulEndpointDiscoveryConfiguration;
import io.gravitee.definition.model.services.dynamicproperty.DynamicPropertyProviderConfiguration;
import io.gravitee.definition.model.services.dynamicproperty.DynamicPropertyService;
import io.gravitee.definition.model.services.dynamicproperty.http.HttpDynamicPropertyProviderConfiguration;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

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

        EndpointDiscoveryService endpointDiscoveryService = api.getService(EndpointDiscoveryService.class);
        Assert.assertNotNull(endpointDiscoveryService);
        Assert.assertNotNull(endpointDiscoveryService.getConfiguration());

        Assert.assertEquals(EndpointDiscoveryProvider.CONSUL, endpointDiscoveryService.getProvider());

        ConsulEndpointDiscoveryConfiguration discoveryProviderConfiguration =
                (ConsulEndpointDiscoveryConfiguration) endpointDiscoveryService.getConfiguration();
        Assert.assertEquals("my-service", discoveryProviderConfiguration.getService());
        Assert.assertEquals("acl", discoveryProviderConfiguration.getAcl());
        Assert.assertEquals("dc", discoveryProviderConfiguration.getDc());
    }

    @Test(expected = JsonMappingException.class)
    public void definition_withEndpointDiscovery_consul_noUrl() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/services/discovery/api-withservice-consul-nourl.json", Api.class);
        api.getServices().get(DynamicPropertyService.class);
    }

    @Test(expected = JsonMappingException.class)
    public void definition_withEndpointDiscovery_consul_noService() throws Exception {
        Api api = load("/io/gravitee/definition/jackson/services/discovery/api-withservice-consul-noservice.json", Api.class);
        api.getServices().get(DynamicPropertyService.class);
    }
}
