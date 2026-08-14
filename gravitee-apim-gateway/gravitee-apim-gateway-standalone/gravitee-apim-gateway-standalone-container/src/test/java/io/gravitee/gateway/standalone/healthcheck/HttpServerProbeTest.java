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
package io.gravitee.gateway.standalone.healthcheck;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.node.api.healthcheck.Result;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.internal.VertxInternal;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetServer;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class HttpServerProbeTest {

    private static final int TIMEOUT_SECONDS = 10;

    private Vertx vertx;
    private NetServer server;
    private CountDownLatch serverSideClose;
    private HttpServerProbe cut;

    @BeforeEach
    void setUp() throws Exception {
        vertx = Vertx.vertx();
        serverSideClose = new CountDownLatch(1);
        server = await(
            vertx
                .createNetServer()
                .connectHandler(socket -> socket.closeHandler(event -> serverSideClose.countDown()))
                .listen(0, "localhost")
        );

        cut = new HttpServerProbe();
        setField(cut, "vertx", vertx);
        setField(cut, "host", "localhost");
        setField(cut, "port", server.actualPort());
    }

    @AfterEach
    void tearDown() throws Exception {
        await(vertx.close());
    }

    @Test
    void should_report_healthy_when_the_server_accepts_the_connection() throws Exception {
        assertThat(check().isHealthy()).isTrue();
    }

    @Test
    void should_report_unhealthy_when_no_server_accepts_the_connection() throws Exception {
        await(server.close());

        assertThat(check().isHealthy()).isFalse();
    }

    @Test
    void should_close_the_connection_it_opened() throws Exception {
        check();

        assertThat(serverSideClose.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void should_not_register_a_new_net_client_on_every_check() throws Exception {
        check();
        long afterFirstCheck = registeredNetClients();

        for (int i = 0; i < 9; i++) {
            check();
        }

        assertThat(registeredNetClients()).isEqualTo(afterFirstCheck);
    }

    private Result check() throws Exception {
        return cut.check().toCompletableFuture().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Vert.x keeps every client it creates in the private {@code children} map of its owner's close future, and
     * only releases them when that owner closes. Counting them is the only way to observe from a test that a
     * client is retained for the whole life of the node rather than released after the check that created it.
     */
    private long registeredNetClients() throws Exception {
        Object closeFuture = ((VertxInternal) vertx).closeFuture();
        Field childrenField = closeFuture.getClass().getDeclaredField("children");
        childrenField.setAccessible(true);
        Map<?, ?> children = (Map<?, ?>) childrenField.get(closeFuture);
        return children == null ? 0 : children.keySet().stream().filter(NetClient.class::isInstance).count();
    }

    private static <T> T await(Future<T> future) throws Exception {
        return future.toCompletionStage().toCompletableFuture().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
