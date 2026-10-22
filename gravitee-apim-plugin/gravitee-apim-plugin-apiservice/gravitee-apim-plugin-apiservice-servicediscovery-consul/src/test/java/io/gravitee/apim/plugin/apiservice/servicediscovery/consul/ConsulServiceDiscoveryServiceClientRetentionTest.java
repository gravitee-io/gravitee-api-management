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

import static fixtures.definition.ApiDefinitionFixtures.anApiV4;
import static io.gravitee.apim.plugin.apiservice.servicediscovery.consul.ConsulServiceDiscoveryService.CONSUL_SERVICE_DISCOVERY_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.endpointgroup.service.EndpointGroupServices;
import io.gravitee.definition.model.v4.service.Service;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;
import io.gravitee.gateway.reactive.api.helper.PluginConfigurationHelper;
import io.gravitee.gateway.reactive.core.v4.endpoint.EndpointManager;
import io.gravitee.gateway.reactive.handlers.api.v4.Api;
import io.gravitee.node.api.configuration.Configuration;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.internal.CloseFuture;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.internal.VertxInternal;
import io.vertx.ext.consul.ConsulClientOptions;
import io.vertx.ext.consul.ServiceOptions;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.ext.consul.ConsulClient;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.testcontainers.consul.ConsulContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Guards the shared-client workaround of {@link ConsulServiceDiscoveryService}: the Consul client of a stopped service must
 * leave the close future of the context that created it, once its connection is closed.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ConsulServiceDiscoveryServiceClientRetentionTest {

    private static final int REDEPLOYS = 10;
    private static final String GROUP = "default-group";
    private static final String SERVICE_NAME = "retention-service";

    static ConsulContainer consulContainer = new ConsulContainer(DockerImageName.parse("consul:1.15.1"));

    private Vertx vertx;
    private Api api;
    private Environment environment;
    private PluginConfigurationHelper pluginConfigurationHelper;

    @BeforeAll
    static void startConsul() {
        consulContainer.start();
    }

    @AfterAll
    static void stopConsul() {
        consulContainer.stop();
    }

    @BeforeEach
    void setUp() {
        vertx = Vertx.vertx();
        // A real service, so that every watch receives a result over an open connection before it is stopped.
        ConsulClient consul = ConsulClient.create(
            vertx,
            new ConsulClientOptions().setHost(consulContainer.getHost()).setPort(consulContainer.getFirstMappedPort())
        );
        consul
            .registerService(new ServiceOptions().setName(SERVICE_NAME).setId("id1").setAddress("10.0.0.1").setPort(8048))
            .blockingAwait(10, TimeUnit.SECONDS);
        consul.close();

        api = new Api(
            anApiV4()
                .toBuilder()
                .endpointGroups(
                    List.of(
                        EndpointGroup.builder()
                            .name(GROUP)
                            .type("http-proxy")
                            .services(
                                EndpointGroupServices.builder()
                                    .discovery(
                                        Service.builder()
                                            .enabled(true)
                                            .type(CONSUL_SERVICE_DISCOVERY_ID)
                                            .configuration(
                                                "{\"url\":\"http://%s:%d\",\"service\":\"%s\"}".formatted(
                                                    consulContainer.getHost(),
                                                    consulContainer.getFirstMappedPort(),
                                                    SERVICE_NAME
                                                )
                                            )
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                )
                .build()
        );
        environment = mock(Environment.class);
        when(environment.getProperty(eq("api.pending_requests_timeout"), eq(Long.class), anyLong())).thenReturn(1000L);
        pluginConfigurationHelper = new PluginConfigurationHelper(mock(Configuration.class), new GraviteeMapper());
    }

    @AfterEach
    void tearDown() {
        vertx.rxClose().blockingAwait(10, TimeUnit.SECONDS);
    }

    @Test
    void should_not_retain_the_clients_of_stopped_services_off_a_vertx_context() throws Exception {
        CloseFuture root = rootCloseFuture();
        int before = retained(root);
        AtomicInteger watched = new AtomicInteger();

        for (int i = 0; i < REDEPLOYS; i++) {
            EndpointManager endpointManager = mock(EndpointManager.class);
            var service = service(endpointManager);
            service.start().blockingAwait();
            awaitDiscovered(endpointManager);
            watched.incrementAndGet();
            service.stop().blockingAwait();
        }

        assertThat(watched).hasValue(REDEPLOYS);
        assertThat(awaitRetained(root, before) - before).isZero();
    }

    @Test
    void should_not_retain_the_clients_of_stopped_services_on_a_deployment_context() throws Exception {
        AtomicReference<CloseFuture> closeFuture = new AtomicReference<>();
        AtomicInteger before = new AtomicInteger();
        List<EndpointManager> endpointManagers = new ArrayList<>();
        List<ConsulServiceDiscoveryService> services = new ArrayList<>();

        vertx
            .getDelegate()
            .deployVerticle(
                new AbstractVerticle() {
                    @Override
                    public void start() {
                        closeFuture.set(((ContextInternal) context).closeFuture());
                        before.set(retained(closeFuture.get()));
                        for (int i = 0; i < REDEPLOYS; i++) {
                            EndpointManager endpointManager = mock(EndpointManager.class);
                            var service = service(endpointManager);
                            service.start().blockingAwait();
                            endpointManagers.add(endpointManager);
                            services.add(service);
                        }
                    }
                }
            )
            .await(10, TimeUnit.SECONDS);

        endpointManagers.forEach(ConsulServiceDiscoveryServiceClientRetentionTest::awaitDiscovered);
        // Stopped from the test thread, as the deployer does, while the watch connections are still open.
        services.forEach(service -> service.stop().blockingAwait());

        assertThat(closeFuture.get()).isNotSameAs(rootCloseFuture());
        assertThat(awaitRetained(closeFuture.get(), before.get()) - before.get()).isZero();
    }

    @Test
    void should_give_each_watcher_its_own_shared_client() {
        // A fixed name would make every watcher share the client, hence the options, of the first one created.
        var first = service(mock(EndpointManager.class));
        var second = service(mock(EndpointManager.class));
        first.start().blockingAwait();
        second.start().blockingAwait();
        try {
            assertThat(vertx.getDelegate().sharedData().<String, Object>getLocalMap("__vertx.shared.httpClients").keySet())
                .hasSize(2)
                .allMatch(name -> name.startsWith("service-discovery-consul-"));
        } finally {
            first.stop().blockingAwait();
            second.stop().blockingAwait();
        }
    }

    private ConsulServiceDiscoveryService service(EndpointManager endpointManager) {
        DeploymentContext deploymentContext = mock(DeploymentContext.class);
        when(deploymentContext.getComponent(Api.class)).thenReturn(api);
        when(deploymentContext.getComponent(Vertx.class)).thenReturn(vertx);
        when(deploymentContext.getComponent(PluginConfigurationHelper.class)).thenReturn(pluginConfigurationHelper);
        when(deploymentContext.getComponent(EndpointManager.class)).thenReturn(endpointManager);
        when(deploymentContext.getComponent(Environment.class)).thenReturn(environment);
        return new ConsulServiceDiscoveryService(deploymentContext, api.getDefinition().getEndpointGroups());
    }

    private static void awaitDiscovered(EndpointManager endpointManager) {
        verify(endpointManager, timeout(10_000)).addOrUpdateEndpoint(eq(GROUP), any());
    }

    private CloseFuture rootCloseFuture() {
        return ((VertxInternal) vertx.getDelegate()).closeFuture();
    }

    /** Waits for close() to complete asynchronously and returns the count observed once it settles or the wait expires. */
    private static int awaitRetained(CloseFuture closeFuture, int expected) {
        try {
            return Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(20, TimeUnit.MILLISECONDS)
                .until(() -> retained(closeFuture), retained -> retained == expected);
        } catch (ConditionTimeoutException e) {
            return retained(closeFuture);
        }
    }

    private static int retained(CloseFuture closeFuture) {
        try {
            Field children = CloseFuture.class.getDeclaredField("children");
            children.setAccessible(true);
            Map<?, ?> map = (Map<?, ?>) children.get(closeFuture);
            return map == null ? 0 : map.size();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                "CloseFuture.children is gone: Vert.x internals changed, this does not mean the leak is fixed. " +
                    "Re-check how closed clients are tracked before touching the shared-client workaround.",
                e
            );
        }
    }
}
