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
package io.gravitee.gateway.reactive.standalone.vertx;

import static io.gravitee.common.http.HttpStatusCode.SERVICE_UNAVAILABLE_503;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.reactive.reactor.HttpRequestDispatcher;
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import io.gravitee.node.api.certificate.TrustStoreLoaderOptions;
import io.gravitee.node.api.server.DefaultServerManager;
import io.gravitee.node.api.server.ServerManager;
import io.gravitee.node.certificates.DefaultKeyStoreLoaderFactoryRegistry;
import io.gravitee.node.vertx.server.http.VertxHttpServer;
import io.gravitee.node.vertx.server.http.VertxHttpServerFactory;
import io.gravitee.node.vertx.server.http.VertxHttpServerOptions;
import io.reactivex.rxjava3.core.Completable;
import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rxjava3.core.http.HttpServerRequest;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class HttpProtocolVerticleTest {

    final ServerManager serverManager = new DefaultServerManager();

    private HttpRequestDispatcher mockRequestDispatcher;

    @BeforeEach
    @DisplayName("Deploy a new http protocol verticle")
    void deployVerticle(Vertx vertx, VertxTestContext testContext) {
        final VertxHttpServerFactory vertxHttpServerFactory = new VertxHttpServerFactory(
            io.vertx.rxjava3.core.Vertx.newInstance(vertx),
            new DefaultKeyStoreLoaderFactoryRegistry<>(),
            new DefaultKeyStoreLoaderFactoryRegistry<>()
        );
        VertxHttpServerOptions httpOptions = VertxHttpServerOptions
            .builder()
            .id("UnitTest")
            .port(0)
            .keyStoreLoaderOptions(KeyStoreLoaderOptions.builder().build())
            .trustStoreLoaderOptions(TrustStoreLoaderOptions.builder().build())
            .build();
        serverManager.register(vertxHttpServerFactory.create(httpOptions));

        mockRequestDispatcher = spy(new DummyHttpRequestDispatcher());
        vertx.deployVerticle(new HttpProtocolVerticle(serverManager, mockRequestDispatcher), testContext.succeedingThenComplete());
    }

    @AfterEach
    @DisplayName("Check that the verticle is still there")
    void lastChecks(Vertx vertx) {
        assertThat(vertx.deploymentIDs()).isNotEmpty().hasSize(1);
    }

    @Test
    void http_server_should_listen(Vertx vertx, VertxTestContext testContext) {
        HttpClient client = vertx.createHttpClient();
        client
            .request(HttpMethod.GET, actualPort(), "127.0.0.1", "/")
            .compose(HttpClientRequest::send)
            .onComplete(
                testContext.succeeding(response ->
                    testContext.verify(() -> {
                        assertThat(response.statusCode()).isEqualTo(HttpStatusCode.OK_200);
                        testContext.completeNow();
                    })
                )
            );
    }

    @Test
    void http_server_should_close_and_resume_on_error(Vertx vertx, VertxTestContext testContext) {
        doReturn(Completable.error(new RuntimeException("error")))
            .doCallRealMethod()
            .when(mockRequestDispatcher)
            .dispatch(any(), anyString());
        HttpClient client = vertx.createHttpClient();
        client
            .request(HttpMethod.GET, actualPort(), "127.0.0.1", "/")
            .compose(HttpClientRequest::send)
            .onComplete(
                testContext.succeeding(response ->
                    testContext.verify(() -> assertThat(response.statusCode()).isEqualTo(HttpStatusCode.INTERNAL_SERVER_ERROR_500))
                )
            )
            .compose(httpClientResponse -> client.request(HttpMethod.GET, actualPort(), "127.0.0.1", "/"))
            .compose(HttpClientRequest::send)
            .onComplete(
                testContext.succeeding(response ->
                    testContext.verify(() -> {
                        assertThat(response.statusCode()).isEqualTo(HttpStatusCode.OK_200);
                        testContext.completeNow();
                    })
                )
            );
    }

    @Test
    void http_server_should_dispose_when_connection_closed(Vertx vertx, VertxTestContext testContext) {
        doReturn(Completable.timer(2, TimeUnit.SECONDS).doOnDispose(testContext::completeNow))
            .when(mockRequestDispatcher)
            .dispatch(any(), anyString());
        HttpClient client = vertx.createHttpClient();
        client
            .request(HttpMethod.GET, actualPort(), "127.0.0.1", "/")
            .compose(request -> {
                request.send().otherwiseEmpty();
                return request.connection().close();
            });
    }

    @Test
    void http_server_should_ignore_already_ended_response_on_error(Vertx vertx, VertxTestContext testContext) {
        doAnswer(invocation -> {
                HttpServerRequest httpServerRequest = invocation.getArgument(0);
                return httpServerRequest
                    .response()
                    .setStatusCode(SERVICE_UNAVAILABLE_503)
                    .rxEnd()
                    .andThen(Completable.error(new RuntimeException("error")));
            })
            .doCallRealMethod()
            .when(mockRequestDispatcher)
            .dispatch(any(), anyString());

        HttpClient client = vertx.createHttpClient();
        client
            .request(HttpMethod.GET, actualPort(), "127.0.0.1", "/")
            .compose(HttpClientRequest::send)
            .onComplete(
                testContext.succeeding(response ->
                    testContext.verify(() -> assertThat(response.statusCode()).isEqualTo(SERVICE_UNAVAILABLE_503))
                )
            )
            .compose(httpClientResponse -> client.request(HttpMethod.GET, actualPort(), "127.0.0.1", "/"))
            .compose(HttpClientRequest::send)
            .onComplete(
                testContext.succeeding(response ->
                    testContext.verify(() -> {
                        assertThat(response.statusCode()).isEqualTo(HttpStatusCode.OK_200);
                        testContext.completeNow();
                    })
                )
            );
    }

    private int actualPort() {
        return serverManager.servers(VertxHttpServer.class).getFirst().instances().getFirst().actualPort();
    }
}
