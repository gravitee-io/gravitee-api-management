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
package io.gravitee.rest.api.service.notifiers.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.node.api.configuration.Configuration;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServer;
import io.vertx.core.internal.VertxInternal;
import java.lang.reflect.Field;
import java.util.Map;
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
class WebNotifierServiceImplTest {

    private static final int TIMEOUT_SECONDS = 10;

    private Vertx vertx;
    private HttpServer server;
    private String url;
    private WebNotifierServiceImpl cut;

    @BeforeEach
    void setUp() throws Exception {
        vertx = Vertx.vertx();
        server = await(
            vertx
                .createHttpServer()
                .requestHandler(req -> req.response().setStatusCode(200).end("{}"))
                .listen(0, "localhost")
        );
        url = "http://localhost:" + server.actualPort() + "/hook";

        Configuration configuration = mock(Configuration.class);
        when(configuration.getProperty(any(String.class), any(), any(Integer.class))).thenReturn(TIMEOUT_SECONDS * 1000);
        cut = new WebNotifierServiceImpl(configuration);
        Field vertxField = WebNotifierServiceImpl.class.getDeclaredField("vertx");
        vertxField.setAccessible(true);
        vertxField.set(cut, vertx);
    }

    @AfterEach
    void tearDown() throws Exception {
        await(vertx.close());
    }

    @Test
    void should_deliver_the_notification_through_a_single_client() throws Exception {
        cut.request(HttpMethod.POST, url, Map.of(), "{}", false);

        assertThat(registeredHttpClients()).isEqualTo(1);
    }

    @Test
    void should_not_register_a_new_client_on_every_notification() throws Exception {
        cut.request(HttpMethod.POST, url, Map.of(), "{}", false);
        long afterFirstNotification = registeredHttpClients();

        for (int i = 0; i < 9; i++) {
            cut.request(HttpMethod.POST, url, Map.of(), "{}", false);
        }

        assertThat(registeredHttpClients()).isEqualTo(afterFirstNotification);
    }

    /**
     * Vert.x keeps every client it creates in the private {@code children} map of its owner's close future, and
     * only releases them when that owner closes. Counting them is the only way to observe from a test that a
     * client is retained for the whole life of the node rather than released after the call that created it.
     */
    private long registeredHttpClients() throws Exception {
        Object closeFuture = ((VertxInternal) vertx).closeFuture();
        Field childrenField = closeFuture.getClass().getDeclaredField("children");
        childrenField.setAccessible(true);
        Map<?, ?> children = (Map<?, ?>) childrenField.get(closeFuture);
        return children == null ? 0 : children.keySet().stream().filter(HttpClient.class::isInstance).count();
    }

    private static <T> T await(Future<T> future) throws Exception {
        return future.toCompletionStage().toCompletableFuture().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }
}
