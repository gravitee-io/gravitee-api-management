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
package io.gravitee.apim.plugin.apiservice.servicediscovery.consul;

import static fixtures.ApiFixtures.anApiWithDefaultGroup;
import static io.gravitee.apim.plugin.apiservice.servicediscovery.consul.ConsulServiceDiscoveryService.CONSUL_SERVICE_DISCOVERY_ID;
import static io.gravitee.apim.plugin.apiservice.servicediscovery.consul.factory.configuration.EndpointConfigurationFactory.CONSUL_METADATA_PATH;
import static io.gravitee.apim.plugin.apiservice.servicediscovery.consul.factory.configuration.EndpointConfigurationFactory.CONSUL_METADATA_SSL;
import static io.gravitee.apim.plugin.apiservice.servicediscovery.consul.factory.configuration.EndpointConfigurationFactory.CONSUL_METADATA_TENANT;
import static io.gravitee.apim.plugin.apiservice.servicediscovery.consul.factory.configuration.EndpointConfigurationFactory.CONSUL_METADATA_WEIGHT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.common.http.GraviteeHttpHeader;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.service.Service;
import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;
import io.gravitee.gateway.reactive.api.helper.PluginConfigurationHelper;
import io.gravitee.gateway.reactive.core.v4.endpoint.DefaultEndpointManager;
import io.gravitee.gateway.reactive.core.v4.endpoint.EndpointManager;
import io.gravitee.gateway.reactive.core.v4.endpoint.ManagedEndpoint;
import io.gravitee.gateway.reactive.handlers.api.v4.Api;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.endpoint.internal.DefaultEndpointConnectorClassLoaderFactory;
import io.gravitee.plugin.endpoint.internal.DefaultEndpointConnectorPluginManager;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.consul.ConsulClientOptions;
import io.vertx.ext.consul.ServiceOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.ext.consul.ConsulClient;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.testcontainers.consul.ConsulContainer;
import org.testcontainers.utility.DockerImageName;

@ExtendWith(value = { MockitoExtension.class, VertxExtension.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConsulServiceDiscoveryServiceIntegrationTest {

    private static final String HTTP_PROXY = "http-proxy";
    private static final String SERVICE_NAME = "my-service";

    static ConsulContainer consulContainer = new ConsulContainer(DockerImageName.parse("consul:1.15.1"));

    private final Vertx vertx = Vertx.vertx();
    private final PluginConfigurationHelper pluginConfigurationHelper = new PluginConfigurationHelper(
        mock(Configuration.class),
        new GraviteeMapper()
    );

    @Mock
    private DeploymentContext deploymentContext;

    @Mock
    private Environment environment;

    private ConsulClient client;
    private Api api;
    private EndpointManager endpointManager;
    private ConsulServiceDiscoveryService cut;

    @BeforeAll
    void beforeAll() {
        consulContainer.start();
        client =
            ConsulClient.create(
                vertx,
                new ConsulClientOptions().setHost(consulContainer.getHost()).setPort(consulContainer.getFirstMappedPort())
            );
    }

    @AfterAll
    void afterAll() {
        consulContainer.stop();
    }

    @BeforeEach
    void setUp() {
        when(deploymentContext.getComponent(Vertx.class)).thenReturn(vertx);
        when(deploymentContext.getComponent(PluginConfigurationHelper.class)).thenReturn(pluginConfigurationHelper);
        when(deploymentContext.getTemplateEngine()).thenReturn(TemplateEngine.templateEngine());

        when(environment.getProperty(eq("api.pending_requests_timeout"), eq(Long.class), anyLong())).thenReturn(1000L);
        when(deploymentContext.getComponent(Environment.class)).thenReturn(environment);
    }

    @AfterEach
    void tearDown() {
        cut.stop().blockingAwait();
    }

    @Nested
    @ExtendWith(value = { VertxExtension.class })
    class HttpProxyBackend {

        private static final String SERVICE_PROXY_SSL_AND_PATH = "service_ssl_path";

        @BeforeEach
        void setUp() throws Exception {
            api = anApiWithDefaultGroup(HTTP_PROXY);
            when(deploymentContext.getComponent(Api.class)).thenReturn(api);

            endpointManager =
                new DefaultEndpointManager(
                    api.getDefinition(),
                    endpointConnectorPluginManager(EndpointBuilder.build(HTTP_PROXY, HttpProxyEndpointConnectorFactory.class)),
                    deploymentContext
                )
                    .start();
            when(deploymentContext.getComponent(EndpointManager.class)).thenReturn(endpointManager);
        }

        @Test
        void should_create_an_endpoint_in_group(VertxTestContext testContext) throws Exception {
            var endpointAdded = testContext.checkpoint();
            var group = api.getDefinition().getEndpointGroups().get(0);

            configureDiscoveryInGroup(group, new JsonObject(Map.of("url", consulUrl(), "service", SERVICE_NAME)));
            registerServiceInConsul(new ServiceOptions().setName(SERVICE_NAME).setId("id1").setAddress("10.0.0.1").setPort(8048));

            endpointManager.addListener((event, endpoint) -> {
                if (event == EndpointManager.Event.ADD) {
                    endpointAdded.flag();
                    return;
                }
                testContext.failNow("Unexpected event");
            });

            startConsulServiceDiscovery(testContext);

            var endpoints = endpointManager.all();
            assertThat(endpoints)
                .extracting(ManagedEndpoint::getDefinition)
                .hasSize(1)
                .contains(
                    Endpoint
                        .builder()
                        .name("consul#id1")
                        .type(HTTP_PROXY)
                        .configuration(new JsonObject(Map.of("target", "http://10.0.0.1:8048/")).toString())
                        .inheritConfiguration(false)
                        .build()
                );
        }

        @Test
        void should_create_an_endpoint_in_group_with_inherited_configuration(VertxTestContext testContext) throws Exception {
            var endpointAdded = testContext.checkpoint();
            var group = api.getDefinition().getEndpointGroups().get(0);

            configureSharedConfigurationInGroup(
                group,
                new JsonObject(Map.ofEntries(Map.entry("proxy", new JsonObject(Map.of("enabled", "false"))))).toString()
            );
            configureDiscoveryInGroup(group, new JsonObject(Map.of("url", consulUrl(), "service", SERVICE_NAME)));
            registerServiceInConsul(new ServiceOptions().setName(SERVICE_NAME).setId("id1").setAddress("10.0.0.1").setPort(8048));

            endpointManager.addListener((event, endpoint) -> {
                if (event == EndpointManager.Event.ADD) {
                    endpointAdded.flag();
                    return;
                }
                testContext.failNow("Unexpected event");
            });

            startConsulServiceDiscovery(testContext);

            var endpoints = endpointManager.all();
            assertThat(endpoints)
                .extracting(ManagedEndpoint::getDefinition)
                .hasSize(1)
                .contains(
                    Endpoint
                        .builder()
                        .name("consul#id1")
                        .type(HTTP_PROXY)
                        .configuration(new JsonObject(Map.of("target", "http://10.0.0.1:8048/")).toString())
                        .inheritConfiguration(true)
                        .build()
                );
        }

        @Test
        void should_create_an_endpoint_with_metadata(VertxTestContext testContext) throws Exception {
            var endpointAdded = testContext.checkpoint();
            var group = api.getDefinition().getEndpointGroups().get(0);

            configureDiscoveryInGroup(group, new JsonObject(Map.of("url", consulUrl(), "service", SERVICE_PROXY_SSL_AND_PATH)));
            registerServiceInConsul(
                new ServiceOptions()
                    .setName(SERVICE_PROXY_SSL_AND_PATH)
                    .setId("id1")
                    .setAddress("10.0.0.1")
                    .setPort(8048)
                    .setMeta(
                        Map.of(
                            CONSUL_METADATA_SSL,
                            "true",
                            CONSUL_METADATA_PATH,
                            "/my-path",
                            CONSUL_METADATA_WEIGHT,
                            "2",
                            CONSUL_METADATA_TENANT,
                            "my-tenant"
                        )
                    )
            );

            endpointManager.addListener((event, endpoint) -> {
                if (event == EndpointManager.Event.ADD) {
                    endpointAdded.flag();
                    return;
                }
                testContext.failNow("Unexpected event");
            });

            startConsulServiceDiscovery(testContext);

            var endpoints = endpointManager.all();
            assertThat(endpoints)
                .extracting(ManagedEndpoint::getDefinition)
                .hasSize(1)
                .contains(
                    Endpoint
                        .builder()
                        .name("consul#id1")
                        .type(HTTP_PROXY)
                        .weight(2)
                        .tenants(List.of("my-tenant"))
                        .configuration(new JsonObject(Map.of("target", "https://10.0.0.1:8048/my-path")).toString())
                        .build()
                );
        }

        @Test
        void should_remove_an_endpoint_in_group_when_service_is_deregistered(VertxTestContext testContext) throws Exception {
            var endpointAdded = testContext.checkpoint();
            var serviceDeregistered = testContext.checkpoint();
            var endpointRemoved = testContext.checkpoint();
            var group = api.getDefinition().getEndpointGroups().get(0);

            configureDiscoveryInGroup(group, new JsonObject(Map.of("url", consulUrl(), "service", SERVICE_NAME)));
            registerServiceInConsul(new ServiceOptions().setName(SERVICE_NAME).setId("id1").setAddress("10.0.0.1").setPort(8048));

            endpointManager.addListener((event, endpoint) -> {
                switch (event) {
                    case ADD:
                        endpointAdded.flag();
                        client.deregisterService("id1").doOnComplete(serviceDeregistered::flag).subscribeOn(Schedulers.io()).subscribe();
                        break;
                    case REMOVE:
                        endpointRemoved.flag();
                        break;
                    default:
                        testContext.failNow("Unexpected event");
                }
            });

            startConsulServiceDiscovery(testContext);

            var endpoints = endpointManager.all();
            assertThat(endpoints).isEmpty();
        }

        @Test
        void should_remove_and_add_endpoint_in_group_when_service_is_updated(VertxTestContext testContext) throws Exception {
            var endpointAdded = testContext.checkpoint(2);
            var serviceUpdated = testContext.checkpoint();
            var endpointRemoved = testContext.checkpoint();
            var group = api.getDefinition().getEndpointGroups().get(0);

            configureDiscoveryInGroup(group, new JsonObject(Map.of("url", consulUrl(), "service", SERVICE_NAME)));
            registerServiceInConsul(new ServiceOptions().setName(SERVICE_NAME).setId("id1").setAddress("10.0.0.1").setPort(8048));

            var updateSent = new AtomicBoolean(false);
            endpointManager.addListener((event, endpoint) -> {
                switch (event) {
                    case ADD:
                        endpointAdded.flag();

                        if (!updateSent.get()) {
                            updateSent.set(true);
                            client
                                .registerService(
                                    new ServiceOptions().setName(SERVICE_NAME).setId("id1").setAddress("10.0.0.2").setPort(8048)
                                )
                                .doOnComplete(serviceUpdated::flag)
                                .subscribeOn(Schedulers.io())
                                .subscribe();
                        }
                        break;
                    case REMOVE:
                        endpointRemoved.flag();
                        break;
                    default:
                        testContext.failNow("Unexpected event");
                }
            });

            startConsulServiceDiscovery(testContext);

            var endpoints = endpointManager.all();
            assertThat(endpoints)
                .extracting(ManagedEndpoint::getDefinition)
                .hasSize(1)
                .contains(
                    Endpoint
                        .builder()
                        .name("consul#id1")
                        .type(HTTP_PROXY)
                        .configuration(new JsonObject(Map.of("target", "http://10.0.0.2:8048/")).toString())
                        .build()
                );
        }

        private String consulUrl() {
            return "http://" + consulContainer.getHost() + ":" + consulContainer.getFirstMappedPort();
        }

        private void configureDiscoveryInGroup(EndpointGroup group, JsonObject configuration) {
            group
                .getServices()
                .setDiscovery(
                    Service.builder().enabled(true).type(CONSUL_SERVICE_DISCOVERY_ID).configuration(configuration.toString()).build()
                );
        }

        private void configureSharedConfigurationInGroup(EndpointGroup group, String sharedConfiguration) {
            group.setSharedConfiguration(sharedConfiguration);
        }

        private void registerServiceInConsul(ServiceOptions service) {
            client.registerService(service).blockingAwait();
        }

        private DefaultEndpointConnectorPluginManager endpointConnectorPluginManager(
            EndpointConnectorPlugin<?, ?> endpointConnectorPlugin
        ) {
            var pluginManager = new DefaultEndpointConnectorPluginManager(
                new DefaultEndpointConnectorClassLoaderFactory(),
                pluginConfigurationHelper
            );
            pluginManager.register(endpointConnectorPlugin);
            return pluginManager;
        }

        private void startConsulServiceDiscovery(VertxTestContext testContext) throws InterruptedException {
            cut = new ConsulServiceDiscoveryServiceFactory().createService(deploymentContext);
            cut
                .start()
                .andThen(
                    Completable.create(emitter -> {
                        testContext.awaitCompletion(5, TimeUnit.SECONDS);
                        emitter.onComplete();
                    })
                )
                .test()
                .await()
                .assertComplete();
        }
    }
}
