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
import static helper.Base64Helper.encodeFileToBase64;
import static io.gravitee.apim.plugin.apiservice.servicediscovery.consul.ConsulServiceDiscoveryService.CONSUL_SERVICE_DISCOVERY_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
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
import io.vertx.core.json.JsonObject;
import io.vertx.ext.consul.ConsulClientOptions;
import io.vertx.ext.consul.ServiceOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.ext.consul.ConsulClient;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

@ExtendWith(value = { MockitoExtension.class, VertxExtension.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConsulServiceDiscoveryServiceWithTLSIntegrationTest {

    private static final String CONSUL_SERVICE = "consul-server";
    private static final int CONSUL_SERVICE_PORT = 8500;
    private static final int CONSUL_SERVICE_SECURED_PORT = 8501;

    private static final String HTTP_PROXY = "http-proxy";
    private static final String SERVICE_NAME = "my-service";

    static DockerComposeContainer<?> consulEnvironment = new DockerComposeContainer<>(
        new File("src/test/resources/docker/consul_with_tls.yml")
    )
        .withExposedService(CONSUL_SERVICE, CONSUL_SERVICE_PORT, Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(30)))
        .withExposedService(
            CONSUL_SERVICE,
            CONSUL_SERVICE_SECURED_PORT,
            Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(30))
        );
    private final Vertx vertx = Vertx.vertx();
    private final PluginConfigurationHelper pluginConfigurationHelper = new PluginConfigurationHelper(
        mock(Configuration.class),
        new GraviteeMapper()
    );

    @Mock
    private DeploymentContext deploymentContext;

    private ConsulClient client;
    private Api api;
    private EndpointManager endpointManager;
    private ConsulServiceDiscoveryService cut;

    @BeforeAll
    void beforeAll() {
        consulEnvironment.start();
        client = ConsulClient.create(vertx, new ConsulClientOptions().setHost(consulHost()).setPort(consulPort()));
    }

    @AfterAll
    void afterAll() {
        consulEnvironment.stop();
    }

    @BeforeEach
    void setUp() throws Exception {
        when(deploymentContext.getComponent(Vertx.class)).thenReturn(vertx);
        when(deploymentContext.getComponent(PluginConfigurationHelper.class)).thenReturn(pluginConfigurationHelper);
        when(deploymentContext.getTemplateEngine()).thenReturn(TemplateEngine.templateEngine());

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

        Environment environment = mock(Environment.class);
        when(environment.getProperty(eq("api.pending_requests_timeout"), eq(Long.class), anyLong())).thenReturn(10000L);
        when(deploymentContext.getComponent(Environment.class)).thenReturn(environment);
    }

    @AfterEach
    void tearDown() {
        cut.stop().blockingAwait();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("generator")
    void should_get_updates_from_a_secured_consul_server(String name, JsonObject config, VertxTestContext testContext) throws Exception {
        var endpointAdded = testContext.checkpoint();
        var newService = new ServiceOptions().setName(SERVICE_NAME).setId("id1").setAddress("10.0.0.1").setPort(8048);

        configureDiscoveryInGroup(config);
        client.registerService(newService).blockingAwait();

        endpointManager.addListener((event, endpoint) -> {
            if (event == EndpointManager.Event.ADD) {
                endpointAdded.flag();
                return;
            }
            testContext.failNow("Unexpected event");
        });
        startConsulServiceDiscovery(testContext);

        // assert
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
                    .build()
            );
    }

    private static Stream<Arguments> generator() throws IOException {
        return Stream.of(
            Arguments.of(
                "using a jks file",
                new JsonObject(
                    Map.ofEntries(
                        Map.entry("url", consulUrl()),
                        Map.entry("service", SERVICE_NAME),
                        Map.entry(
                            "ssl",
                            new JsonObject(
                                Map.ofEntries(
                                    Map.entry(
                                        "trustStore",
                                        new JsonObject(
                                            Map.ofEntries(
                                                Map.entry("type", "JKS"),
                                                Map.entry("path", "src/test/resources/docker/config/ssl/client-truststore.jks"),
                                                Map.entry("password", "gravitee")
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            Arguments.of(
                "using a jks content",
                new JsonObject(
                    Map.ofEntries(
                        Map.entry("url", consulUrl()),
                        Map.entry("service", SERVICE_NAME),
                        Map.entry(
                            "ssl",
                            new JsonObject(
                                Map.ofEntries(
                                    Map.entry(
                                        "trustStore",
                                        new JsonObject(
                                            Map.ofEntries(
                                                Map.entry("type", "JKS"),
                                                Map.entry(
                                                    "content",
                                                    encodeFileToBase64(
                                                        Path.of("src/test/resources/docker/config/ssl/client-truststore.jks")
                                                    )
                                                ),
                                                Map.entry("password", "gravitee")
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            Arguments.of(
                "using a pkcs12 file",
                new JsonObject(
                    Map.ofEntries(
                        Map.entry("url", consulUrl()),
                        Map.entry("service", SERVICE_NAME),
                        Map.entry(
                            "ssl",
                            new JsonObject(
                                Map.ofEntries(
                                    Map.entry(
                                        "trustStore",
                                        new JsonObject(
                                            Map.ofEntries(
                                                Map.entry("type", "PKCS12"),
                                                Map.entry("path", "src/test/resources/docker/config/ssl/client-truststore.p12"),
                                                Map.entry("password", "gravitee")
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            Arguments.of(
                "using a pkcs12 content",
                new JsonObject(
                    Map.ofEntries(
                        Map.entry("url", consulUrl()),
                        Map.entry("service", SERVICE_NAME),
                        Map.entry(
                            "ssl",
                            new JsonObject(
                                Map.ofEntries(
                                    Map.entry(
                                        "trustStore",
                                        new JsonObject(
                                            Map.ofEntries(
                                                Map.entry("type", "PKCS12"),
                                                Map.entry(
                                                    "content",
                                                    encodeFileToBase64(
                                                        Path.of("src/test/resources/docker/config/ssl/client-truststore.p12")
                                                    )
                                                ),
                                                Map.entry("password", "gravitee")
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            Arguments.of(
                "using a pem file",
                new JsonObject(
                    Map.ofEntries(
                        Map.entry("url", consulUrl()),
                        Map.entry("service", SERVICE_NAME),
                        Map.entry(
                            "ssl",
                            new JsonObject(
                                Map.ofEntries(
                                    Map.entry(
                                        "trustStore",
                                        new JsonObject(
                                            Map.ofEntries(
                                                Map.entry("type", "PEM"),
                                                Map.entry("path", "src/test/resources/docker/config/ssl/client-truststore.pem")
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            Arguments.of(
                "using a pem content",
                new JsonObject(
                    Map.ofEntries(
                        Map.entry("url", consulUrl()),
                        Map.entry("service", SERVICE_NAME),
                        Map.entry(
                            "ssl",
                            new JsonObject(
                                Map.ofEntries(
                                    Map.entry(
                                        "trustStore",
                                        new JsonObject(
                                            Map.ofEntries(
                                                Map.entry("type", "PEM"),
                                                Map.entry(
                                                    "content",
                                                    Files.readString(Path.of("src/test/resources/docker/config/ssl/client-truststore.pem"))
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        );
    }

    private static String consulUrl() {
        return ("https://" + consulHost() + ":" + consulSecuredPort());
    }

    private static String consulHost() {
        return consulEnvironment.getServiceHost(CONSUL_SERVICE, CONSUL_SERVICE_SECURED_PORT);
        //        return "localhost";
    }

    private static int consulPort() {
        return consulEnvironment.getServicePort(CONSUL_SERVICE, CONSUL_SERVICE_PORT);
        //        return CONSUL_SERVICE_PORT;
    }

    private static int consulSecuredPort() {
        return consulEnvironment.getServicePort(CONSUL_SERVICE, CONSUL_SERVICE_SECURED_PORT);
        //        return CONSUL_SERVICE_SECURED_PORT;
    }

    private void configureDiscoveryInGroup(JsonObject configuration) {
        api
            .getDefinition()
            .getEndpointGroups()
            .get(0)
            .getServices()
            .setDiscovery(
                Service.builder().enabled(true).type(CONSUL_SERVICE_DISCOVERY_ID).configuration(configuration.toString()).build()
            );
    }

    private DefaultEndpointConnectorPluginManager endpointConnectorPluginManager(EndpointConnectorPlugin<?, ?> endpointConnectorPlugin) {
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
