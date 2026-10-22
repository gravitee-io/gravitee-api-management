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
package io.gravitee.plugin.endpoint.tcp.proxy.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.reactive.api.context.tcp.TcpExecutionContext;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.plugin.configurations.tcp.TcpTarget;
import io.gravitee.plugin.endpoint.tcp.proxy.configuration.TcpProxyEndpointConnectorConfiguration;
import io.gravitee.plugin.endpoint.tcp.proxy.configuration.TcpProxyEndpointConnectorSharedConfiguration;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.internal.CloseFuture;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.internal.VertxInternal;
import io.vertx.core.net.NetClientOptions;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.net.NetClient;
import io.vertx.rxjava3.core.net.NetServer;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * Guards the close future unregistration {@link TcpClientFactory} performs on the client it creates.
 *
 * <p>The workaround leans on Vert.x internals ({@code CleanableNetClient.unwrap()}, {@code CloseFuture.remove}): these
 * tests fail loudly if their shape changes, and {@link #should_still_leak_when_the_client_comes_straight_from_vertx()}
 * goes red once the gateway runs on a Vert.x carrying the upstream fix (5.2), at which point the workaround and this
 * class can both be deleted.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class TcpClientFactoryCloseFutureTest {

    private static final int REDEPLOYS = 20;

    private Vertx vertx;
    private NetServer server;
    private TcpExecutionContext ctx;

    @BeforeEach
    void setUp() {
        vertx = Vertx.vertx();
        server = vertx
            .createNetServer()
            .connectHandler(socket -> {})
            .rxListen(0, "localhost")
            .blockingGet();
        ctx = mock(TcpExecutionContext.class);
        when(ctx.getComponent(Vertx.class)).thenReturn(vertx);
        when(ctx.getComponent(Configuration.class)).thenReturn(mock(Configuration.class));
    }

    @AfterEach
    void tearDown() {
        server.rxClose().onErrorComplete().blockingAwait();
        vertx.rxClose().blockingAwait(10, TimeUnit.SECONDS);
    }

    @Test
    void should_not_retain_the_clients_it_creates_off_a_vertx_context() {
        CloseFuture root = rootCloseFuture();
        int before = retained(root);

        for (int i = 0; i < REDEPLOYS; i++) {
            buildThenClose();
        }

        assertThat(retained(root) - before).isZero();
    }

    @Test
    void should_not_retain_the_clients_it_creates_on_a_deployment_context() throws Exception {
        // The production case: the client is created on the event loop serving the first connection, so it lands on the
        // close future of the server verticle, which the gateway never undeploys.
        AtomicReference<CloseFuture> closeFuture = new AtomicReference<>();
        AtomicReference<Integer> retainedDelta = new AtomicReference<>();

        vertx
            .getDelegate()
            .deployVerticle(
                new AbstractVerticle() {
                    @Override
                    public void start() {
                        closeFuture.set(((ContextInternal) context).closeFuture());
                        int before = retained(closeFuture.get());
                        for (int i = 0; i < REDEPLOYS; i++) {
                            buildThenClose();
                        }
                        retainedDelta.set(retained(closeFuture.get()) - before);
                    }
                }
            )
            .await(10, TimeUnit.SECONDS);

        assertThat(closeFuture.get()).isNotSameAs(rootCloseFuture());
        assertThat(retainedDelta).hasValue(0);
    }

    @Test
    void should_return_a_client_that_still_connects_after_being_unregistered() {
        TcpClientFactory factory = new TcpClientFactory();
        NetClient client = factory.getOrBuildTcpClient(ctx, configuration(), new TcpProxyEndpointConnectorSharedConfiguration());
        try {
            assertThat(client.rxConnect(server.actualPort(), "localhost").blockingGet()).isNotNull();
        } finally {
            factory.close();
        }
    }

    @Test
    void should_still_leak_when_the_client_comes_straight_from_vertx() throws Exception {
        // This watches the Vert.x version the gateway is built against (vertx.version in the root pom), on an event loop
        // context like the one the production leak happens on.
        AtomicReference<Integer> delta = new AtomicReference<>();

        vertx
            .getDelegate()
            .deployVerticle(
                new AbstractVerticle() {
                    @Override
                    public void start(Promise<Void> started) {
                        CloseFuture closeFuture = ((ContextInternal) context).closeFuture();
                        int before = retained(closeFuture);
                        List<Future<Void>> closes = new ArrayList<>();
                        for (int i = 0; i < REDEPLOYS; i++) {
                            closes.add(vertx.createNetClient(new NetClientOptions()).close());
                        }
                        Future.all(closes).onComplete(done -> {
                            delta.set(retained(closeFuture) - before);
                            started.complete();
                        });
                    }
                }
            )
            .await(10, TimeUnit.SECONDS);

        assertThat(delta)
            .as(
                "[vertx-5.0-close-future-leak] Vert.x no longer leaks closed clients: remove releaseFromOwnerCloseFuture from TcpClientFactory and this test"
            )
            .hasValue(REDEPLOYS);
    }

    private void buildThenClose() {
        TcpClientFactory factory = new TcpClientFactory();
        factory.getOrBuildTcpClient(ctx, configuration(), new TcpProxyEndpointConnectorSharedConfiguration());
        factory.close();
    }

    private TcpProxyEndpointConnectorConfiguration configuration() {
        TcpProxyEndpointConnectorConfiguration configuration = new TcpProxyEndpointConnectorConfiguration();
        TcpTarget target = new TcpTarget();
        target.setHost("localhost");
        target.setPort(server.actualPort());
        configuration.setTcpTarget(target);
        return configuration;
    }

    private CloseFuture rootCloseFuture() {
        return ((VertxInternal) vertx.getDelegate()).closeFuture();
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
                    "Re-check how closed clients are tracked before touching releaseFromOwnerCloseFuture.",
                e
            );
        }
    }
}
