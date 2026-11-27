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
package io.gravitee.gateway.services.endpoint.discovery.verticle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.event.Event;
import io.gravitee.common.event.impl.SimpleEvent;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.*;
import io.gravitee.definition.model.services.discovery.EndpointDiscoveryService;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.discovery.api.ServiceDiscovery;
import io.gravitee.discovery.api.event.EventType;
import io.gravitee.discovery.api.event.Handler;
import io.gravitee.discovery.api.service.Service;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.gravitee.gateway.services.endpoint.discovery.factory.ServiceDiscoveryFactory;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.discovery.ServiceDiscoveryPlugin;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class EndpointDiscoveryVerticleTest {

    private static final String API_ID = "api-id";

    @InjectMocks
    EndpointDiscoveryVerticle endpointDiscoveryVerticle;

    @Mock
    ConfigurablePluginManager<ServiceDiscoveryPlugin> serviceDiscoveryPluginManager;

    @Mock
    ServiceDiscoveryFactory serviceDiscoveryFactory;

    @BeforeEach
    public void setup() {
        when(serviceDiscoveryPluginManager.get(any())).thenReturn(mock(ServiceDiscoveryPlugin.class));
        endpointDiscoveryVerticle.setMapper(new GraviteeMapper());
    }

    @Test
    public void shoudlCreateSimpleDiscoveredService() {
        String serviceId = "DISCOVERY:SERVICE:ID";
        String serviceHost = "DISCOVERY_SERVICE_HOST";
        int servicePort = 8080;
        String serviceScheme = "http";

        String expectedServiceName = "sd#DISCOVERY#SERVICE#ID";
        String expectedServiceTarget = "http://DISCOVERY_SERVICE_HOST:8080/";

        String expectedConfiguration = initConfiguration(expectedServiceName, expectedServiceTarget, false);

        testDiscoveryVerticle(
            serviceId,
            serviceHost,
            servicePort,
            serviceScheme,
            expectedServiceName,
            expectedServiceTarget,
            expectedConfiguration
        );
    }

    @Test
    public void shoudlCreateHttpsDiscoveredService() {
        String serviceId = "DISCOVERY:SERVICE:ID";
        String serviceHost = "DISCOVERY_SERVICE_HOST";
        int servicePort = 443;
        String serviceScheme = "https";

        String expectedServiceName = "sd#DISCOVERY#SERVICE#ID";
        String expectedServiceTarget = "https://DISCOVERY_SERVICE_HOST:443/";

        String expectedConfiguration = initConfiguration(expectedServiceName, expectedServiceTarget, true);

        testDiscoveryVerticle(
            serviceId,
            serviceHost,
            servicePort,
            serviceScheme,
            expectedServiceName,
            expectedServiceTarget,
            expectedConfiguration
        );
    }

    @Test
    public void should_stop_v2_service_discovery_when_api_is_updated_with_a_v4_definition() throws Exception {
        ServiceDiscovery serviceDiscovery = deploy(v2ApiWithServiceDiscovery());

        endpointDiscoveryVerticle.onEvent(new SimpleEvent<>(ReactorEvent.UPDATE, v4Api(API_ID)));

        verify(serviceDiscovery).stop();
    }

    @Test
    public void should_stop_v2_service_discovery_when_api_is_undeployed_with_a_v4_definition() throws Exception {
        ServiceDiscovery serviceDiscovery = deploy(v2ApiWithServiceDiscovery());

        endpointDiscoveryVerticle.onEvent(new SimpleEvent<>(ReactorEvent.UNDEPLOY, v4Api(API_ID)));

        verify(serviceDiscovery).stop();
    }

    @Test
    public void should_stop_v2_service_discovery_once_when_api_is_updated_then_undeployed_with_a_v4_definition() throws Exception {
        ServiceDiscovery serviceDiscovery = deploy(v2ApiWithServiceDiscovery());

        endpointDiscoveryVerticle.onEvent(new SimpleEvent<>(ReactorEvent.UPDATE, v4Api(API_ID)));
        endpointDiscoveryVerticle.onEvent(new SimpleEvent<>(ReactorEvent.UNDEPLOY, v4Api(API_ID)));

        verify(serviceDiscovery, times(1)).stop();
    }

    @Test
    public void should_keep_v2_service_discovery_when_another_v4_api_is_undeployed() throws Exception {
        ServiceDiscovery serviceDiscovery = deploy(v2ApiWithServiceDiscovery());

        endpointDiscoveryVerticle.onEvent(new SimpleEvent<>(ReactorEvent.UNDEPLOY, v4Api("another-api-id")));

        verify(serviceDiscovery, never()).stop();
    }

    @Test
    public void should_keep_v2_service_discovery_when_a_v4_api_is_deployed() throws Exception {
        ServiceDiscovery serviceDiscovery = deploy(v2ApiWithServiceDiscovery());

        endpointDiscoveryVerticle.onEvent(new SimpleEvent<>(ReactorEvent.DEPLOY, v4Api(API_ID)));

        verify(serviceDiscovery, never()).stop();
    }

    @Test
    public void should_restart_v2_service_discovery_when_v2_api_is_updated() throws Exception {
        Api api = v2ApiWithServiceDiscovery();
        ServiceDiscovery serviceDiscovery = deploy(api);

        endpointDiscoveryVerticle.onEvent(new SimpleEvent<>(ReactorEvent.UPDATE, api));

        verify(serviceDiscovery).stop();
        verify(serviceDiscovery, times(2)).listen(any());
    }

    @Test
    public void should_stop_v2_service_discovery_when_v2_api_is_undeployed() throws Exception {
        Api api = v2ApiWithServiceDiscovery();
        ServiceDiscovery serviceDiscovery = deploy(api);

        endpointDiscoveryVerticle.onEvent(new SimpleEvent<>(ReactorEvent.UNDEPLOY, api));

        verify(serviceDiscovery).stop();
    }

    private static Api v2ApiWithServiceDiscovery() {
        EndpointDiscoveryService endpointDiscoveryService = new EndpointDiscoveryService();
        endpointDiscoveryService.setEnabled(true);

        EndpointGroup group = new EndpointGroup();
        group.getServices().set(Collections.singletonList(endpointDiscoveryService));

        Proxy proxy = new Proxy();
        proxy.setGroups(Collections.singleton(group));

        io.gravitee.definition.model.Api definition = new io.gravitee.definition.model.Api();
        definition.setId(API_ID);
        definition.setProxy(proxy);
        Api api = new Api(definition);
        api.setEnabled(true);
        return api;
    }

    private ServiceDiscovery deploy(Api api) throws Exception {
        ServiceDiscovery serviceDiscovery = mock(ServiceDiscovery.class);
        when(serviceDiscoveryFactory.create(any(), any())).thenReturn(serviceDiscovery);

        endpointDiscoveryVerticle.onEvent(new SimpleEvent<>(ReactorEvent.DEPLOY, api));

        verify(serviceDiscovery).listen(any());
        return serviceDiscovery;
    }

    private static Reactable v4Api(String apiId) {
        return new io.gravitee.gateway.reactive.handlers.api.v4.Api(
            io.gravitee.definition.model.v4.Api.builder().id(apiId).type(ApiType.PROXY).build()
        );
    }

    private void testDiscoveryVerticle(
        String serviceId,
        String serviceHost,
        int servicePort,
        String serviceScheme,
        String expectedServiceName,
        String expectedServiceTarget,
        String expectedConfiguration
    ) {
        EndpointDiscoveryService endpointDiscoveryService = new EndpointDiscoveryService();
        endpointDiscoveryService.setEnabled(true);

        HttpProxy httpProxy = new HttpProxy();
        httpProxy.setEnabled(true);
        httpProxy.setHost("HTTP_PROXY_HOST");
        httpProxy.setPort(8181);

        EndpointGroup defaultEndpointGroup = new EndpointGroup();
        defaultEndpointGroup.getServices().set(Collections.singletonList(endpointDiscoveryService));
        defaultEndpointGroup.setHttpProxy(httpProxy);

        Proxy proxy = new Proxy();
        proxy.setGroups(Collections.singleton(defaultEndpointGroup));

        io.gravitee.definition.model.Api definition = new io.gravitee.definition.model.Api();
        Api deployedApi = new Api(definition);
        deployedApi.setEnabled(true);
        definition.setProxy(proxy);

        ServiceDiscovery serviceDiscovery = initServiceDiscovery(serviceId, serviceHost, servicePort, serviceScheme);

        when(serviceDiscoveryFactory.create(any(), any())).thenReturn(serviceDiscovery);

        Event deployEvent = new SimpleEvent(ReactorEvent.DEPLOY, deployedApi);

        endpointDiscoveryVerticle.onEvent(deployEvent);

        final Set<Endpoint> endpoints = defaultEndpointGroup.getEndpoints();
        assertNotNull(endpoints);
        assertEquals(1, endpoints.size());
        final Endpoint discoveredEndpoint = endpoints.iterator().next();
        assertEquals(expectedServiceName, discoveredEndpoint.getName());
        assertEquals(expectedServiceTarget, discoveredEndpoint.getTarget());

        assertEquals(expectedConfiguration, discoveredEndpoint.getConfiguration());
    }

    private ServiceDiscovery initServiceDiscovery(String serviceId, String serviceHost, int servicePort, String serviceScheme) {
        return new ServiceDiscovery() {
            @Override
            public void listen(Handler<io.gravitee.discovery.api.event.Event> handler) throws Exception {
                handler.handle(
                    new io.gravitee.discovery.api.event.Event() {
                        public EventType type() {
                            return EventType.REGISTER;
                        }

                        public Service service() {
                            return new Service() {
                                @Override
                                public String id() {
                                    return serviceId;
                                }

                                @Override
                                public String host() {
                                    return serviceHost;
                                }

                                @Override
                                public int port() {
                                    return servicePort;
                                }

                                @Override
                                public String scheme() {
                                    return serviceScheme;
                                }
                            };
                        }
                    }
                );
            }

            @Override
            public void stop() throws Exception {
                // Nothing to do
            }
        };
    }

    private String initConfiguration(String expectedServiceName, String expectedServiceTarget, boolean withSsl) {
        String expectedConfiguration =
            "{" +
            "\"name\":\"" +
            expectedServiceName +
            "\"," +
            "\"target\":\"" +
            expectedServiceTarget +
            "\"," +
            "\"weight\":1," +
            "\"backup\":false," +
            "\"type\":\"http\"," +
            (withSsl
                    ? "\"ssl\":{" +
                    "\"trustAll\":true," +
                    "\"hostnameVerifier\":false," +
                    "\"trustStore\":null," +
                    "\"keyStore\":null" +
                    "},"
                    : "") +
            "\"http\":{" +
            "\"idleTimeout\":60000," +
            "\"keepAliveTimeout\":30000," +
            "\"connectTimeout\":5000," +
            "\"keepAlive\":true," +
            "\"readTimeout\":10000," +
            "\"pipelining\":false," +
            "\"maxConcurrentConnections\":100," +
            "\"useCompression\":true," +
            "\"propagateClientAcceptEncoding\":false," +
            "\"followRedirects\":false," +
            "\"clearTextUpgrade\":false," +
            "\"version\":\"HTTP_1_1\",\"maxHeaderSize\":8192,\"maxChunkSize\":8192" +
            "}," +
            "\"proxy\":{" +
            "\"enabled\":true," +
            "\"useSystemProxy\":false," +
            "\"host\":\"HTTP_PROXY_HOST\"," +
            "\"port\":8181," +
            "\"username\":null," +
            "\"password\":null," +
            "\"type\":\"HTTP\"" +
            "}" +
            "}";
        return expectedConfiguration;
    }
}
