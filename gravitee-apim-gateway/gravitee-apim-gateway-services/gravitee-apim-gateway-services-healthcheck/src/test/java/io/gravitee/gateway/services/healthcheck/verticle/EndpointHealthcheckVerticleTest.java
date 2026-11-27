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
package io.gravitee.gateway.services.healthcheck.verticle;

import static fixtures.definition.ApiDefinitionFixtures.aHttpProxyApiV4;
import static fixtures.definition.ApiDefinitionFixtures.anApiV2;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.event.impl.SimpleEvent;
import io.gravitee.common.util.ObservableSet;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.definition.model.EndpointGroup;
import io.gravitee.definition.model.endpoint.HttpEndpoint;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.gravitee.gateway.services.healthcheck.EndpointHealthcheckResolver;
import io.gravitee.gateway.services.healthcheck.EndpointRule;
import io.gravitee.gateway.services.healthcheck.context.HealthCheckContext;
import io.gravitee.gateway.services.healthcheck.context.HealthCheckContextFactory;
import io.gravitee.gateway.services.healthcheck.rule.EndpointRuleHandler;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EndpointHealthcheckVerticleTest {

    private static final String API_ID = "api-id";

    private static final HttpEndpoint ENDPOINT = HttpEndpoint.builder().name("default").target("http://localhost/hc").build();

    @Mock
    private Vertx vertx;

    @Mock
    private EndpointHealthcheckResolver endpointResolver;

    @Mock
    private HealthCheckContextFactory healthCheckContextFactory;

    @Mock
    private GatewayConfiguration gatewayConfiguration;

    @Mock
    private EndpointRule<HttpEndpoint> rule;

    @Mock
    private EndpointRuleHandler<HttpEndpoint> runner;

    @Mock
    private EndpointRule<HttpEndpoint> otherRule;

    @Mock
    private EndpointRuleHandler<HttpEndpoint> otherRunner;

    @InjectMocks
    private EndpointHealthcheckVerticle verticle;

    @BeforeEach
    void setUp() throws Exception {
        verticle.init(vertx, mock(Context.class));
        lenient().when(healthCheckContextFactory.create(any(), any())).thenReturn(mock(HealthCheckContext.class));
        lenient().when(rule.endpoint()).thenReturn(ENDPOINT);
        lenient().when(rule.createRunner(eq(vertx), eq(rule), any(), any())).thenReturn(runner);
    }

    @Test
    void should_stop_v2_health_check_when_api_is_updated_with_a_v4_definition() throws Exception {
        deployV2Api();

        verticle.onEvent(new SimpleEvent<>(ReactorEvent.UPDATE, v4Api(API_ID)));

        verify(runner).close();
    }

    @Test
    void should_stop_v2_health_check_when_api_is_undeployed_with_a_v4_definition() throws Exception {
        deployV2Api();

        verticle.onEvent(new SimpleEvent<>(ReactorEvent.UNDEPLOY, v4Api(API_ID)));

        verify(runner).close();
    }

    @Test
    void should_stop_v2_health_check_once_when_api_is_updated_then_undeployed_with_a_v4_definition() throws Exception {
        deployV2Api();

        verticle.onEvent(new SimpleEvent<>(ReactorEvent.UPDATE, v4Api(API_ID)));
        verticle.onEvent(new SimpleEvent<>(ReactorEvent.UNDEPLOY, v4Api(API_ID)));

        verify(runner, times(1)).close();
    }

    @Test
    void should_keep_v2_health_check_when_another_v4_api_is_undeployed() throws Exception {
        deployV2Api();

        verticle.onEvent(new SimpleEvent<>(ReactorEvent.UNDEPLOY, v4Api("another-api-id")));

        verify(runner, never()).close();
    }

    @Test
    void should_keep_v2_health_check_when_a_v4_api_is_deployed() throws Exception {
        deployV2Api();

        verticle.onEvent(new SimpleEvent<>(ReactorEvent.DEPLOY, v4Api(API_ID)));

        verify(runner, never()).close();
    }

    @Test
    void should_restart_v2_health_check_when_v2_api_is_updated() throws Exception {
        Api api = deployV2Api();

        verticle.onEvent(new SimpleEvent<>(ReactorEvent.UPDATE, api));

        verify(runner).close();
        verify(rule, times(2)).createRunner(eq(vertx), eq(rule), any(), any());
    }

    @Test
    void should_stop_v2_health_check_when_v2_api_is_undeployed() throws Exception {
        Api api = deployV2Api();

        verticle.onEvent(new SimpleEvent<>(ReactorEvent.UNDEPLOY, api));

        verify(runner).close();
    }

    @Test
    void should_stop_only_the_health_check_of_an_endpoint_removed_from_a_v2_api() throws Exception {
        ObservableSet<Endpoint> endpoints = new ObservableSet<>(new LinkedHashSet<>());
        Api api = v2Api(endpoints);
        when(endpointResolver.resolve(api)).thenReturn(List.of());
        verticle.onEvent(new SimpleEvent<>(ReactorEvent.DEPLOY, api));

        HttpEndpoint otherEndpoint = HttpEndpoint.builder().name("other").target("http://localhost/other").build();
        when(rule.api()).thenReturn(api);
        when(endpointResolver.resolve(api, ENDPOINT)).thenReturn(rule);
        when(otherRule.api()).thenReturn(api);
        when(otherRule.endpoint()).thenReturn(otherEndpoint);
        when(otherRule.createRunner(eq(vertx), eq(otherRule), any(), any())).thenReturn(otherRunner);
        when(endpointResolver.resolve(api, otherEndpoint)).thenReturn(otherRule);
        endpoints.add(otherEndpoint);
        endpoints.add(ENDPOINT);

        endpoints.remove(ENDPOINT);

        verify(runner).close();
        verify(otherRunner, never()).close();
    }

    @Test
    void should_stop_health_check_of_an_endpoint_added_after_the_api_is_undeployed_with_a_v4_definition() throws Exception {
        ObservableSet<Endpoint> endpoints = new ObservableSet<>(new LinkedHashSet<>());
        Api api = v2Api(endpoints);
        when(endpointResolver.resolve(api)).thenReturn(List.of());
        verticle.onEvent(new SimpleEvent<>(ReactorEvent.DEPLOY, api));
        verticle.onEvent(new SimpleEvent<>(ReactorEvent.UNDEPLOY, v4Api(API_ID)));

        when(rule.api()).thenReturn(api);
        when(endpointResolver.resolve(api, ENDPOINT)).thenReturn(rule);
        endpoints.add(ENDPOINT);

        verify(runner).close();
    }

    private Api deployV2Api() throws Exception {
        Api api = v2Api(Set.of());
        when(rule.api()).thenReturn(api);
        when(endpointResolver.resolve(api)).thenReturn(List.of(rule));

        verticle.onEvent(new SimpleEvent<>(ReactorEvent.DEPLOY, api));

        verify(rule).createRunner(eq(vertx), eq(rule), any(), any());
        return api;
    }

    private static Api v2Api(Set<Endpoint> endpoints) {
        EndpointGroup group = new EndpointGroup();
        group.setName("default-group");
        group.setEndpoints(endpoints);
        io.gravitee.definition.model.Api definition = anApiV2();
        definition.setId(API_ID);
        definition.getProxy().setGroups(Set.of(group));
        return new Api(definition);
    }

    private static Reactable v4Api(String apiId) {
        return new io.gravitee.gateway.reactive.handlers.api.v4.Api(aHttpProxyApiV4(apiId));
    }
}
