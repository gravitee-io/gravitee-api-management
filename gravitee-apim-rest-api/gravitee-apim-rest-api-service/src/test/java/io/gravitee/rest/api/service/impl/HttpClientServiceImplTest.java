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
package io.gravitee.rest.api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.node.api.configuration.Configuration;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.internal.VertxInternal;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ExecutionException;
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
class HttpClientServiceImplTest {

    private static final int TIMEOUT_SECONDS = 10;
    private static final String ORGANIZATION_ID = "DEFAULT";
    private static final String OTHER_ORGANIZATION_ID = "OTHER";

    private Vertx vertx;
    private HttpServer server;
    private String baseUrl;
    private HttpClientServiceImpl cut;

    @BeforeEach
    void setUp() throws Exception {
        vertx = Vertx.vertx();
        server = await(
            vertx
                .createHttpServer()
                .requestHandler(req -> {
                    if (req.path().startsWith("/hang")) {
                        return; // accept the request and never answer it
                    }
                    req.response().setStatusCode(req.path().startsWith("/ko") ? 500 : 200).end("{}");
                })
                .listen(0, "localhost")
        );
        baseUrl = "http://localhost:" + server.actualPort();

        Configuration configuration = mock(Configuration.class);
        when(configuration.getProperty(any(String.class), any(), any(Integer.class))).thenReturn(TIMEOUT_SECONDS * 1000);
        when(configuration.getProperty(any(String.class), any(String.class))).thenReturn("HTTP");
        cut = new HttpClientServiceImpl(configuration, vertx);
    }

    @AfterEach
    void tearDown() throws Exception {
        await(vertx.close());
    }

    @Test
    void should_return_the_response_body() {
        assertThat(cut.request(ORGANIZATION_ID, HttpMethod.GET, baseUrl + "/ok", null, null, false).toString()).isEqualTo("{}");
    }

    @Test
    void should_share_a_client_between_calls_with_the_same_scheme_and_proxy() {
        assertThat(cut.createHttpClient(ORGANIZATION_ID, "http", false)).isSameAs(cut.createHttpClient(ORGANIZATION_ID, "http", false));
    }

    @Test
    void should_use_a_distinct_client_per_scheme_and_proxy() {
        HttpClient plain = cut.createHttpClient(ORGANIZATION_ID, "http", false);

        assertThat(cut.createHttpClient(ORGANIZATION_ID, "https", false)).isNotSameAs(plain);
        assertThat(cut.createHttpClient(ORGANIZATION_ID, "http", true)).isNotSameAs(plain);
    }

    @Test
    void should_not_register_a_new_client_on_every_successful_call() throws Exception {
        cut.request(ORGANIZATION_ID, HttpMethod.GET, baseUrl + "/ok", null, null, false);
        long afterFirstCall = registeredHttpClients();

        for (int i = 0; i < 9; i++) {
            cut.request(ORGANIZATION_ID, HttpMethod.GET, baseUrl + "/ok", null, null, false);
        }

        assertThat(registeredHttpClients()).isEqualTo(afterFirstCall);
    }

    @Test
    void should_not_register_a_new_client_on_every_failed_call() throws Exception {
        assertThatThrownBy(() -> cut.request(ORGANIZATION_ID, HttpMethod.GET, baseUrl + "/ko", null, null, false)).isNotNull();
        long afterFirstCall = registeredHttpClients();

        for (int i = 0; i < 9; i++) {
            assertThatThrownBy(() -> cut.request(ORGANIZATION_ID, HttpMethod.GET, baseUrl + "/ko", null, null, false)).isNotNull();
        }

        assertThat(registeredHttpClients()).isEqualTo(afterFirstCall);
    }

    @Test
    void should_release_the_connection_to_a_server_that_never_answers() throws Exception {
        HttpServer silentServer = await(
            vertx
                .createHttpServer()
                .requestHandler(req -> {})
                .listen(0, "localhost")
        );
        Configuration shortTimeout = mock(Configuration.class);
        when(shortTimeout.getProperty(any(String.class), any(), any(Integer.class))).thenReturn(500);
        HttpClient client = new HttpClientServiceImpl(shortTimeout, vertx).createHttpClient(ORGANIZATION_ID, "http", false);

        Future<?> call = client
            .request(new RequestOptions().setHost("localhost").setPort(silentServer.actualPort()).setURI("/"))
            .compose(request -> request.send());

        assertThatThrownBy(() -> await(call)).isInstanceOf(ExecutionException.class);
    }

    @Test
    void should_use_a_distinct_client_per_organization() {
        assertThat(cut.createHttpClient(OTHER_ORGANIZATION_ID, "http", false)).isNotSameAs(
            cut.createHttpClient(ORGANIZATION_ID, "http", false)
        );
    }

    @Test
    void should_share_the_client_of_an_organization_between_its_calls() {
        assertThat(cut.createHttpClient(OTHER_ORGANIZATION_ID, "http", false)).isSameAs(
            cut.createHttpClient(OTHER_ORGANIZATION_ID, "http", false)
        );
    }

    @Test
    void should_use_a_client_of_its_own_for_the_endpoints_the_platform_owns() {
        assertThat(cut.createHttpClient(null, "http", false)).isNotSameAs(cut.createHttpClient(ORGANIZATION_ID, "http", false));
        assertThat(cut.createHttpClient(null, "http", false)).isSameAs(cut.createHttpClient(null, "http", false));
    }

    @Test
    void should_not_let_an_unresponsive_endpoint_delay_the_calls_of_another_organization() throws Exception {
        Configuration configuration = mock(Configuration.class);
        when(configuration.getProperty(any(String.class), any(), any(Integer.class))).thenReturn(3000);
        HttpClientServiceImpl service = new HttpClientServiceImpl(configuration, vertx);
        HttpClient saturated = service.createHttpClient(OTHER_ORGANIZATION_ID, "http", false);

        // far more than the connections of a client, so that the calls of the organization pile up
        for (int i = 0; i < 40; i++) {
            saturated
                .request(new RequestOptions().setHost("localhost").setPort(server.actualPort()).setURI("/hang"))
                .compose(request -> request.send());
        }
        Thread.sleep(300);

        long start = System.nanoTime();
        service.request(ORGANIZATION_ID, HttpMethod.GET, baseUrl + "/ok", null, null, false);

        assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)).isLessThan(1500);
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
