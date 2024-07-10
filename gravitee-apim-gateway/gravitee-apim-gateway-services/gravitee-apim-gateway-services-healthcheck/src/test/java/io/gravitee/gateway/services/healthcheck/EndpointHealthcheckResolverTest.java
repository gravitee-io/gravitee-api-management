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
package io.gravitee.gateway.services.healthcheck;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.gravitee.definition.model.*;
import io.gravitee.definition.model.endpoint.HttpEndpoint;
import io.gravitee.definition.model.services.Services;
import io.gravitee.definition.model.services.healthcheck.HealthCheckService;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.vertx.core.json.JsonObject;
import java.util.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class EndpointHealthcheckResolverTest {

    @Mock
    private GatewayConfiguration mockGatewayConfiguration;

    @Mock
    private Api mockApi;

    @Mock
    private io.gravitee.definition.model.Api mockApiDefinition;

    @Mock
    private Proxy mockProxy;

    @Mock
    private EndpointGroup mockEndpointGroup;

    @Mock
    private HttpEndpoint mockEndpoint;

    @InjectMocks
    private EndpointHealthcheckResolver endpointHealthcheckResolver = new EndpointHealthcheckResolver();

    @Before
    public void before() throws JsonProcessingException {
        reset();
        when(mockApi.getId()).thenReturn("api-id");
        when(mockApiDefinition.getProxy()).thenReturn(mockProxy);
        when(mockApi.getDefinition()).thenReturn(mockApiDefinition);

        when(mockProxy.getGroups()).thenReturn(Collections.singleton(mockEndpointGroup));

        when(mockEndpointGroup.getEndpoints()).thenReturn(new HashSet<>(Arrays.asList(mockEndpoint, mock(Endpoint.class))));

        when(mockEndpoint.getType()).thenReturn("http");
        when(mockEndpoint.getConfiguration())
            .thenReturn(
                "{\n" +
                "  \"name\": \"test\",\n" +
                "  \"target\": \"http://localhost\",\n" +
                "  \"http\": {\n" +
                "    \"connectTimeout\": 5000,\n" +
                "    \"idleTimeout\": 60000,\n" +
                "    \"keepAliveTimeout\": 30000,\n" +
                "    \"keepAlive\": true,\n" +
                "    \"readTimeout\": 10000,\n" +
                "    \"pipelining\": false,\n" +
                "    \"maxConcurrentConnections\": 100,\n" +
                "    \"useCompression\": true,\n" +
                "    \"followRedirects\": false\n" +
                "  }\n" +
                "}\n"
            );
        when(mockGatewayConfiguration.tenant()).thenReturn(Optional.empty());
    }

    /**
     * Cases when no HC must be run :
     * no global HC && no local HC
     * global HC disabled && no local HC
     * global HC disabled && local HC disabled
     * global HC enabled && local HC disabled
     * no global HC && local HC disabled
     */

    @Test
    public void shouldNotResolveEndpointWithoutGlobalNorLocal() {
        Services services = new Services();
        services.set(Collections.emptyList());
        when(mockApiDefinition.getServices()).thenReturn(services);

        List<EndpointRule> resolve = endpointHealthcheckResolver.resolve(mockApi);

        assertNotNull(resolve);
        assertTrue(resolve.isEmpty());
    }

    @Test
    public void shouldNotResolveEndpointWithGlobalDisabledAndNoLocal() {
        Services services = new Services();
        HealthCheckService healthCheckService = new HealthCheckService();
        healthCheckService.setEnabled(false);
        services.set(Collections.singleton(healthCheckService));
        when(mockApiDefinition.getServices()).thenReturn(services);

        List<EndpointRule> resolve = endpointHealthcheckResolver.resolve(mockApi);

        assertNotNull(resolve);
        assertTrue(resolve.isEmpty());
    }

    @Test
    public void shouldNotResolveEndpointWithGlobalDisabledAndLocalDisabled() {
        Services services = new Services();
        HealthCheckService healthCheckService = new HealthCheckService();
        healthCheckService.setEnabled(false);
        services.set(Collections.singleton(healthCheckService));
        when(mockApiDefinition.getServices()).thenReturn(services);
        when(mockEndpoint.getConfiguration())
            .thenReturn(
                "{\n" +
                "  \"name\": \"test\",\n" +
                "  \"target\": \"http://localhost\",\n" +
                "  \"healthcheck\": {\n" +
                "    \"enabled\": false,\n" +
                "    \"inherit\": false\n" +
                "  }, \n" +
                "  \"http\": {\n" +
                "    \"connectTimeout\": 5000,\n" +
                "    \"idleTimeout\": 60000,\n" +
                "    \"keepAliveTimeout\": 30000,\n" +
                "    \"keepAlive\": true,\n" +
                "    \"readTimeout\": 10000,\n" +
                "    \"pipelining\": false,\n" +
                "    \"maxConcurrentConnections\": 100,\n" +
                "    \"useCompression\": true,\n" +
                "    \"followRedirects\": false\n" +
                "  }\n" +
                "}\n"
            );

        List<EndpointRule> resolve = endpointHealthcheckResolver.resolve(mockApi);

        assertNotNull(resolve);
        assertTrue(resolve.isEmpty());
    }

    @Test
    public void shouldNotResolveEndpointWithoutGlobalAndLocalDisabled() {
        Services services = new Services();
        services.set(Collections.emptyList());
        when(mockApiDefinition.getServices()).thenReturn(services);
        when(mockEndpoint.getConfiguration())
            .thenReturn(
                "{\n" +
                "  \"name\": \"test\",\n" +
                "  \"target\": \"http://localhost\",\n" +
                "  \"healthcheck\": {\n" +
                "    \"enabled\": false,\n" +
                "    \"inherit\": false\n" +
                "  }, \n" +
                "  \"http\": {\n" +
                "    \"connectTimeout\": 5000,\n" +
                "    \"idleTimeout\": 60000,\n" +
                "    \"keepAliveTimeout\": 30000,\n" +
                "    \"keepAlive\": true,\n" +
                "    \"readTimeout\": 10000,\n" +
                "    \"pipelining\": false,\n" +
                "    \"maxConcurrentConnections\": 100,\n" +
                "    \"useCompression\": true,\n" +
                "    \"followRedirects\": false\n" +
                "  }\n" +
                "}\n"
            );

        List<EndpointRule> resolve = endpointHealthcheckResolver.resolve(mockApi);

        assertNotNull(resolve);
        assertTrue(resolve.isEmpty());
    }

    @Test
    public void shouldNotResolveEndpointWithGlobalEnabledAndLocalDisabled() {
        Services services = new Services();
        HealthCheckService healthCheckService = new HealthCheckService();
        healthCheckService.setEnabled(true);
        services.set(Collections.singleton(healthCheckService));
        when(mockApiDefinition.getServices()).thenReturn(services);

        when(mockEndpoint.getConfiguration())
            .thenReturn(
                "{\n" +
                "  \"name\": \"test\",\n" +
                "  \"target\": \"http://localhost\",\n" +
                "  \"healthcheck\": {\n" +
                "    \"enabled\": false,\n" +
                "    \"inherit\": false\n" +
                "  }, \n" +
                "  \"http\": {\n" +
                "    \"connectTimeout\": 5000,\n" +
                "    \"idleTimeout\": 60000,\n" +
                "    \"keepAliveTimeout\": 30000,\n" +
                "    \"keepAlive\": true,\n" +
                "    \"readTimeout\": 10000,\n" +
                "    \"pipelining\": false,\n" +
                "    \"maxConcurrentConnections\": 100,\n" +
                "    \"useCompression\": true,\n" +
                "    \"followRedirects\": false\n" +
                "  }\n" +
                "}\n"
            );

        List<EndpointRule> resolve = endpointHealthcheckResolver.resolve(mockApi);

        assertNotNull(resolve);
        assertTrue(resolve.isEmpty());
    }

    /**
     * Cases when HC must be ran :
     * global HC enabled and no local HC
     * global HC enabled and local HC enabled
     * global HC disabled and local HC enabled
     * no global HC and local HC enabled
     */

    @Test
    public void shouldResolveEndpointWithGlobalEnabledAndNoLocal() {
        Services services = new Services();
        HealthCheckService healthCheckService = new HealthCheckService();
        healthCheckService.setEnabled(true);
        services.set(Collections.singleton(healthCheckService));
        when(mockApiDefinition.getServices()).thenReturn(services);

        List<EndpointRule> resolve = endpointHealthcheckResolver.resolve(mockApi);

        assertNotNull(resolve);
        assertEquals(1, resolve.size());
        assertNotNull(resolve.get(0).api());
        assertEquals("api-id", resolve.get(0).api().getId());
    }

    @Test
    public void shouldResolveEndpointWithGlobalEnabledAndLocalEnabled() {
        Services services = new Services();
        HealthCheckService healthCheckService = new HealthCheckService();
        healthCheckService.setEnabled(true);
        services.set(Collections.singleton(healthCheckService));
        when(mockApiDefinition.getServices()).thenReturn(services);
        when(mockEndpoint.getConfiguration())
            .thenReturn(
                "{\n" +
                "  \"name\": \"test\",\n" +
                "  \"target\": \"http://localhost\",\n" +
                "  \"healthcheck\": {\n" +
                "    \"enabled\": true,\n" +
                "    \"inherit\": false,\n" +
                "    \"request\": {" +
                "      \"uri\": \"http://localhost\",\n" +
                "      \"method\": \"GET\",\n" +
                "      \"path\": \"/\"\n" +
                "    }\n" +
                "  }, \n" +
                "  \"http\": {\n" +
                "    \"connectTimeout\": 5000,\n" +
                "    \"idleTimeout\": 60000,\n" +
                "    \"keepAliveTimeout\": 30000,\n" +
                "    \"keepAlive\": true,\n" +
                "    \"readTimeout\": 10000,\n" +
                "    \"pipelining\": false,\n" +
                "    \"maxConcurrentConnections\": 100,\n" +
                "    \"useCompression\": true,\n" +
                "    \"followRedirects\": false\n" +
                "  }\n" +
                "}\n"
            );

        List<EndpointRule> resolve = endpointHealthcheckResolver.resolve(mockApi);

        assertNotNull(resolve);
        assertEquals(1, resolve.size());
        assertNotNull(resolve.get(0).api());
        assertEquals("api-id", resolve.get(0).api().getId());
    }

    @Test
    public void shouldResolveEndpointWithGlobalDisabledAndLocalEnabled() {
        Services services = new Services();
        HealthCheckService healthCheckService = new HealthCheckService();
        healthCheckService.setEnabled(false);
        services.set(Collections.singleton(healthCheckService));
        when(mockApiDefinition.getServices()).thenReturn(services);

        when(mockEndpoint.getConfiguration())
            .thenReturn(
                "{\n" +
                "  \"name\": \"test\",\n" +
                "  \"target\": \"http://localhost\",\n" +
                "  \"healthcheck\": {\n" +
                "    \"enabled\": true,\n" +
                "    \"inherit\": false,\n" +
                "    \"request\": {" +
                "      \"uri\": \"http://localhost\",\n" +
                "      \"method\": \"GET\",\n" +
                "      \"path\": \"/\"\n" +
                "    }\n" +
                "  }, \n" +
                "  \"http\": {\n" +
                "    \"connectTimeout\": 5000,\n" +
                "    \"idleTimeout\": 60000,\n" +
                "    \"keepAliveTimeout\": 30000,\n" +
                "    \"keepAlive\": true,\n" +
                "    \"readTimeout\": 10000,\n" +
                "    \"pipelining\": false,\n" +
                "    \"maxConcurrentConnections\": 100,\n" +
                "    \"useCompression\": true,\n" +
                "    \"followRedirects\": false\n" +
                "  }\n" +
                "}\n"
            );

        List<EndpointRule> resolve = endpointHealthcheckResolver.resolve(mockApi);

        assertNotNull(resolve);
        assertEquals(1, resolve.size());
        assertNotNull(resolve.get(0).api());
        assertEquals("api-id", resolve.get(0).api().getId());
    }

    @Test
    public void shouldResolveEndpointWithoutGlobalAndLocalEnabled() {
        Services services = new Services();
        services.set(Collections.emptyList());
        when(mockApiDefinition.getServices()).thenReturn(services);

        when(mockEndpoint.getConfiguration())
            .thenReturn(
                "{\n" +
                "  \"name\": \"test\",\n" +
                "  \"target\": \"http://localhost\",\n" +
                "  \"healthcheck\": {\n" +
                "    \"enabled\": true,\n" +
                "    \"inherit\": false,\n" +
                "    \"request\": {" +
                "      \"uri\": \"http://localhost\",\n" +
                "      \"method\": \"GET\",\n" +
                "      \"path\": \"/\"\n" +
                "    }\n" +
                "  }, \n" +
                "  \"http\": {\n" +
                "    \"connectTimeout\": 5000,\n" +
                "    \"idleTimeout\": 60000,\n" +
                "    \"keepAliveTimeout\": 30000,\n" +
                "    \"keepAlive\": true,\n" +
                "    \"readTimeout\": 10000,\n" +
                "    \"pipelining\": false,\n" +
                "    \"maxConcurrentConnections\": 100,\n" +
                "    \"useCompression\": true,\n" +
                "    \"followRedirects\": false\n" +
                "  },\n" +
                "  \"ssl\": {\n" +
                "    \"trustAll\": true,\n" +
                "    \"keyStore\": {\n" +
                "      \"type\": \"\"\n" +
                "    }\n" +
                "  }\n" +
                "}\n"
            );

        List<EndpointRule> resolve = endpointHealthcheckResolver.resolve(mockApi);

        assertNotNull(resolve);
        assertEquals(1, resolve.size());
        assertNotNull(resolve.get(0).api());
        assertEquals("api-id", resolve.get(0).api().getId());
    }

    @Test
    public void shouldResolveEndpointUsingGroupProxy() {
        Services services = new Services();
        HealthCheckService healthCheckService = new HealthCheckService();
        healthCheckService.setEnabled(true);
        services.set(Set.of(healthCheckService));

        when(mockApiDefinition.getServices()).thenReturn(services);

        HttpProxy groupProxy = new HttpProxy();
        groupProxy.setHost("localhost");
        groupProxy.setPort(9001);

        when(mockEndpointGroup.getHttpProxy()).thenReturn(groupProxy);

        JsonObject endpointConfig = new JsonObject()
            .put("backup", false)
            .put("inherit", true)
            .put("name", "default")
            .put("weight", 1)
            .put("type", "http")
            .put("target", "http://localhost:2000");

        when(mockEndpoint.getConfiguration()).thenReturn(endpointConfig.encode());

        List<EndpointRule> resolved = endpointHealthcheckResolver.resolve(mockApi);

        assertNotNull(resolved);
        assertEquals(1, resolved.size());

        Endpoint endpoint = resolved.get(0).endpoint();

        assertEquals(HttpEndpoint.class, endpoint.getClass());

        HttpEndpoint httpEndpoint = (HttpEndpoint) endpoint;
        HttpProxy httpProxy = httpEndpoint.getHttpProxy();

        assertNotNull(httpProxy);
        assertEquals("localhost", httpProxy.getHost());
        assertEquals(9001, httpProxy.getPort());
    }
}
