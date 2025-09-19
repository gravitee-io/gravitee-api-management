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

import static fixtures.definition.ApiDefinitionFixtures.anApiV2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import io.gravitee.definition.model.Endpoint;
import io.gravitee.definition.model.EndpointGroup;
import io.gravitee.definition.model.HttpProxy;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.endpoint.HttpEndpoint;
import io.gravitee.definition.model.services.Services;
import io.gravitee.definition.model.services.healthcheck.HealthCheckService;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.handlers.api.definition.Api;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
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

    @BeforeEach
    public void before() {
        lenient().when(mockApi.getId()).thenReturn("api-id");
        lenient().when(mockApiDefinition.getProxy()).thenReturn(mockProxy);
        lenient().when(mockApi.getDefinition()).thenReturn(mockApiDefinition);

        lenient().when(mockProxy.getGroups()).thenReturn(Collections.singleton(mockEndpointGroup));

        lenient().when(mockEndpointGroup.getEndpoints()).thenReturn(new HashSet<>(Arrays.asList(mockEndpoint, mock(Endpoint.class))));

        lenient().when(mockEndpoint.getType()).thenReturn("http");
        lenient()
            .when(mockEndpoint.getConfiguration())
            .thenReturn(
                """
                {
                  "name": "test",
                  "target": "http://localhost",
                  "http": {
                    "connectTimeout": 5000,
                    "idleTimeout": 60000,
                    "keepAliveTimeout": 30000,
                    "keepAlive": true,
                    "readTimeout": 10000,
                    "pipelining": false,
                    "maxConcurrentConnections": 100,
                    "useCompression": true,
                    "followRedirects": false
                  }
                }
                """
            );
        lenient().when(mockGatewayConfiguration.tenant()).thenReturn(Optional.empty());
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
    public void should_not_resolve_endpoint_rules_when_healthcheck_is_not_defined_globally_nor_locally() {
        Services services = new Services();
        services.set(Collections.emptyList());

        var api = anApi(services, Set.of(healthcheckNotDefinedEndpoint()));

        assertThat(endpointHealthcheckResolver.resolve(api)).isEmpty();
    }

    @Test
    public void should_not_resolve_endpoint_rules_when_healthcheck_is_disabled_globally_with_no_endpoint_configuration() {
        HealthCheckService healthCheckService = new HealthCheckService();
        healthCheckService.setEnabled(false);

        Services services = new Services();
        services.set(Collections.singleton(healthCheckService));

        var api = anApi(services, Set.of(healthcheckNotDefinedEndpoint()));

        assertThat(endpointHealthcheckResolver.resolve(api)).isEmpty();
    }

    @Test
    public void should_not_resolve_endpoint_rules_when_healthcheck_is_disabled_globally_and_locally() {
        HealthCheckService healthCheckService = new HealthCheckService();
        healthCheckService.setEnabled(false);

        Services services = new Services();
        services.set(Collections.singleton(healthCheckService));

        var api = anApi(services, Set.of(healthcheckDisabledEndpoint()));

        assertThat(endpointHealthcheckResolver.resolve(api)).isEmpty();
    }

    @Test
    public void should_not_resolve_endpoint_rules_when_healthcheck_is_disabled_locally_without_global_configuration() {
        Services services = new Services();
        services.set(Collections.emptyList());

        var api = anApi(services, Set.of(healthcheckDisabledEndpoint()));

        assertThat(endpointHealthcheckResolver.resolve(api)).isEmpty();
    }

    @Test
    public void should_not_resolve_endpoint_rules_when_healthcheck_is_enabled_globally_but_locally_disabled() {
        HealthCheckService healthCheckService = new HealthCheckService();
        healthCheckService.setEnabled(true);

        Services services = new Services();
        services.set(Collections.singleton(healthCheckService));

        var api = anApi(services, Set.of(healthcheckDisabledEndpoint()));

        assertThat(endpointHealthcheckResolver.resolve(api)).isEmpty();
    }

    /**
     * Cases when HC must be ran :
     * global HC enabled and no local HC
     * global HC enabled and local HC enabled
     * global HC disabled and local HC enabled
     * no global HC and local HC enabled
     */

    @Test
    public void should_resolve_endpoint_rules_when_healthcheck_is_enabled_globally_without_local_configuration() {
        HealthCheckService healthCheckService = new HealthCheckService();
        healthCheckService.setEnabled(true);

        Services services = new Services();
        services.set(Collections.singleton(healthCheckService));

        var api = anApi(services, Set.of(healthcheckNotDefinedEndpoint()));

        assertThat(endpointHealthcheckResolver.resolve(api))
            .hasSize(1)
            .extracting(rule -> rule.api().getId())
            .containsExactly("api-id");
    }

    @Test
    public void should_resolve_endpoint_rules_when_healthcheck_is_enabled_globally_and_locally_enabled() {
        HealthCheckService healthCheckService = new HealthCheckService();
        healthCheckService.setEnabled(true);

        Services services = new Services();
        services.set(Collections.singleton(healthCheckService));

        var api = anApi(services, Set.of(healthcheckEnabledEndpoint()));

        assertThat(endpointHealthcheckResolver.resolve(api))
            .hasSize(1)
            .extracting(rule -> rule.api().getId())
            .containsExactly("api-id");
    }

    @Test
    public void should_resolve_endpoint_rules_when_healthcheck_is_disabled_globally_but_locally_enabled() {
        HealthCheckService healthCheckService = new HealthCheckService();
        healthCheckService.setEnabled(false);

        Services services = new Services();
        services.set(Collections.singleton(healthCheckService));

        var api = anApi(services, Set.of(healthcheckEnabledEndpoint()));

        assertThat(endpointHealthcheckResolver.resolve(api))
            .hasSize(1)
            .extracting(rule -> rule.api().getId())
            .containsExactly("api-id");
    }

    @Test
    public void should_resolve_endpoint_rules_when_healthcheck_is_enabled_locally_without_global_configuration() {
        Services services = new Services();
        services.set(Collections.emptyList());

        var api = anApi(services, Set.of(healthcheckEnabledEndpoint()));

        assertThat(endpointHealthcheckResolver.resolve(api))
            .hasSize(1)
            .extracting(rule -> rule.api().getId())
            .containsExactly("api-id");
    }

    @Test
    public void should_resolve_endpoint_using_group_proxy() {
        HealthCheckService healthCheckService = new HealthCheckService();
        healthCheckService.setEnabled(true);

        Services services = new Services();
        services.set(Set.of(healthCheckService));

        var api = anApi(services, Set.of(healthcheckNotDefinedEndpoint()));

        List<EndpointRule> resolved = endpointHealthcheckResolver.resolve(api);

        assertThat(resolved)
            .hasSize(1)
            .satisfies(rules -> {
                var endpoint = rules.get(0).endpoint();
                assertThat(endpoint)
                    .isInstanceOf(HttpEndpoint.class)
                    .satisfies(httpEndpoint -> {
                        var httpProxy = ((HttpEndpoint) httpEndpoint).getHttpProxy();
                        assertThat(httpProxy).extracting(HttpProxy::getHost, HttpProxy::getPort).containsExactly("localhost", 9001);
                    });
            });
    }

    private Api anApi(Services services, Set<Endpoint> endpoints) {
        var groupProxy = new HttpProxy();
        groupProxy.setHost("localhost");
        groupProxy.setPort(9001);

        var group = new EndpointGroup();
        group.setHttpProxy(groupProxy);
        group.setEndpoints(endpoints);

        var proxy = new Proxy();
        proxy.setGroups(Set.of(group));

        var apiDefinition = anApiV2().toBuilder().id("api-id").proxy(proxy).services(services).build();

        return new Api(apiDefinition);
    }

    private HttpEndpoint healthcheckNotDefinedEndpoint() {
        HttpEndpoint endpoint = new HttpEndpoint("default", "http://localhost");
        endpoint.setConfiguration(
            """
                {
                  "name": "test",
                  "target": "http://localhost:2000",
                  "inherit": true,
                  "http": {
                    "connectTimeout": 5000,
                    "idleTimeout": 60000,
                    "keepAliveTimeout": 60000,
                    "keepAlive": true,
                    "readTimeout": 10000,
                    "pipelining": false,
                    "maxConcurrentConnections": 100,
                    "useCompression": true,
                    "followRedirects": false
                  }
                }
            """
        );
        return endpoint;
    }

    private HttpEndpoint healthcheckDisabledEndpoint() {
        HttpEndpoint endpoint = new HttpEndpoint("default", "http://localhost");
        endpoint.setConfiguration(
            """
                {
                  "name": "test",
                  "target": "http://localhost",
                  "healthcheck": {
                    "enabled": false,
                    "inherit": false
                  },
                  "http": {
                    "connectTimeout": 5000,
                    "idleTimeout": 60000,
                    "keepAliveTimeout": 60000,
                    "keepAlive": true,
                    "readTimeout": 10000,
                    "pipelining": false,
                    "maxConcurrentConnections": 100,
                    "useCompression": true,
                    "followRedirects": false
                  }
                }
            """
        );
        return endpoint;
    }

    private HttpEndpoint healthcheckEnabledEndpoint() {
        HttpEndpoint endpoint = new HttpEndpoint("default", "http://localhost");
        endpoint.setConfiguration(
            """
                {
                 "name": "test",
                 "target": "http://localhost",
                 "healthcheck": {
                   "enabled": true,
                   "schedule": "0 */5 * * * *",
                   "inherit": false,
                   "steps": [
                     {
                       "name": "default-step",
                       "request": {
                         "method": "GET",
                         "path": "/"
                       },
                       "response": {
                         "assertions": ["#response.status == 200"]
                       }
                     }
                   ]
                 },
                 "http": {
                   "connectTimeout": 5000,
                   "idleTimeout": 60000,
                   "keepAliveTimeout": 60000,
                   "keepAlive": true,
                   "readTimeout": 10000,
                   "pipelining": false,
                   "maxConcurrentConnections": 100,
                   "useCompression": true,
                   "followRedirects": false
                 }
               }
            """
        );
        return endpoint;
    }
}
