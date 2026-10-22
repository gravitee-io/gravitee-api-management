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
package io.gravitee.rest.api.management.v2.rest.resource.kafka_console;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.cluster.use_case.SearchClusterUseCase;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.internal.VertxInternal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.TimeoutHandler;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.lang.reflect.Field;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ProxyKafkaConsoleResourceTest {

    private static final int TIMEOUT_SECONDS = 10;
    private static final String USER_ID = "user-id";

    private Vertx vertx;
    private HttpServer server;
    private ProxyKafkaConsoleResource cut;
    private KafkaConsoleClient kafkaConsoleClient;
    private final AtomicInteger openedConnections = new AtomicInteger();
    private final List<HttpServerRequest> heldRequests = new CopyOnWriteArrayList<>();
    private MockEnvironment environment;
    private final AtomicInteger trickledRequests = new AtomicInteger();
    private final List<Map.Entry<AsyncResponse, TimeoutHandler>> timeoutHandlers = new CopyOnWriteArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        vertx = Vertx.vertx();
        server = await(
            vertx
                .createHttpServer()
                .connectionHandler(connection -> openedConnections.incrementAndGet())
                .requestHandler(request -> {
                    if (request.path().endsWith("/held")) {
                        // Answered by the test once it has counted the connections.
                        heldRequests.add(request);
                        return;
                    }
                    if (request.path().endsWith("/trickle")) {
                        // Sends a byte at once, then one every 50 ms: well within the 500 ms idle timeout, which never fires.
                        trickledRequests.incrementAndGet();
                        HttpServerResponse response = request.response().setChunked(true);
                        response.write("x");
                        long timer = vertx.setPeriodic(50, id -> response.write("x"));
                        response.closeHandler(closed -> vertx.cancelTimer(timer));
                        return;
                    }
                    if (request.path().endsWith("/stall")) {
                        // Never answers: the proxy has to give up on its own.
                        return;
                    }
                    if (request.path().endsWith("/echo")) {
                        // Answers with the headers and the body Kafbat received.
                        request
                            .body()
                            .onSuccess(received ->
                                request
                                    .response()
                                    .end(
                                        request
                                                .headers()
                                                .entries()
                                                .stream()
                                                .map(header -> header.getKey().toLowerCase() + "=" + header.getValue())
                                                .sorted()
                                                .collect(Collectors.joining("\n")) +
                                            "\nbody=" +
                                            received
                                    )
                            );
                        return;
                    }
                    request.response().end("ok");
                })
                .listen(0)
        );

        environment = new MockEnvironment()
            .withProperty("kafka.console.enabled", "true")
            .withProperty("kafka.console.server.security.secret", "a-secret-long-enough-for-hmac-256-signatures")
            .withProperty("kafka.console.server.host", "localhost")
            .withProperty("kafka.console.server.port", String.valueOf(server.actualPort()))
            .withProperty("kafka.console.server.timeout", "500");

        SearchClusterUseCase searchClusterUseCase = mock(SearchClusterUseCase.class);
        when(searchClusterUseCase.execute(any())).thenReturn(new SearchClusterUseCase.Output(new Page<>(List.of(), 1, 0, 0)));

        UserEntity user = new UserEntity();
        user.setId(USER_ID);
        user.setFirstname("John");
        user.setLastname("Doe");
        UserService userService = mock(UserService.class);
        when(userService.findById(any(), any())).thenReturn(user);

        Principal principal = () -> USER_ID;
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getUserPrincipal()).thenReturn(principal);

        cut = new ProxyKafkaConsoleResource();
        ReflectionTestUtils.setField(cut, "environment", environment);
        kafkaConsoleClient = new KafkaConsoleClient(vertx, environment);
        ReflectionTestUtils.setField(cut, "kafkaConsoleClient", kafkaConsoleClient);
        ReflectionTestUtils.setField(cut, "searchClusterUseCase", searchClusterUseCase);
        ReflectionTestUtils.setField(cut, "userService", userService);
        ReflectionTestUtils.setField(cut, "securityContext", securityContext);

        GraviteeContext.setCurrentOrganization("DEFAULT");
        GraviteeContext.setCurrentEnvironment("DEFAULT");
    }

    @AfterEach
    void tearDown() throws Exception {
        GraviteeContext.cleanContext();
        kafkaConsoleClient.close();
        await(vertx.close());
    }

    @Test
    void should_reuse_one_http_client_across_proxied_requests() throws Exception {
        for (int i = 0; i < 20; i++) {
            assertThat(proxyGet().getStatus()).isEqualTo(200);
        }

        assertThat(registeredHttpClients()).isEqualTo(1);
    }

    @Test
    void should_cap_the_shared_pool_at_max_connections() throws Exception {
        int requests = KafkaConsoleClient.MAX_CONNECTIONS + 5;
        List<CompletableFuture<Object>> responses = new ArrayList<>();
        for (int i = 0; i < requests; i++) {
            responses.add(proxyAsync("GET", "/api/held", Map.of(), null));
        }

        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS);
        while (heldRequests.size() < KafkaConsoleClient.MAX_CONNECTIONS && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        assertThat(heldRequests).hasSize(KafkaConsoleClient.MAX_CONNECTIONS);
        // The requests beyond the pool size wait for a connection instead of opening one.
        Thread.sleep(200);
        assertThat(openedConnections).hasValue(KafkaConsoleClient.MAX_CONNECTIONS);

        // Answer the held requests, and those that get a connection once they free theirs.
        while (responses.stream().anyMatch(response -> !response.isDone()) && System.currentTimeMillis() < deadline) {
            for (HttpServerRequest request : heldRequests) {
                heldRequests.remove(request);
                request.response().end("ok");
            }
            Thread.sleep(20);
        }
        for (CompletableFuture<Object> response : responses) {
            assertThat(((Response) response.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)).getStatus()).isEqualTo(200);
        }
    }

    @Test
    void should_give_up_on_a_stalled_kafbat_response_and_keep_serving() throws Exception {
        Object stalled = proxy("/api/stall");

        // Vert.x fails the request once the idle timeout elapses, and the pooled connection is released.
        assertThat(stalled).isInstanceOf(Throwable.class);
        assertThat(proxyGet().getStatus()).isEqualTo(200);
    }

    @Test
    void should_free_the_pool_when_the_global_timeout_ends_a_trickling_response() throws Exception {
        // Far beyond the test: neither the trickling exchanges nor the request waiting for a connection give up on their own.
        environment.setProperty("kafka.console.server.timeout", "5000");
        // One more than the pool: the last request waits for a connection when its 504 fires.
        int requests = KafkaConsoleClient.MAX_CONNECTIONS + 1;
        List<CompletableFuture<Object>> responses = new ArrayList<>();
        for (int i = 0; i < requests; i++) {
            responses.add(proxyAsync("GET", "/api/trickle", Map.of(), null));
        }
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS);
        while (trickledRequests.get() < KafkaConsoleClient.MAX_CONNECTIONS && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        assertThat(trickledRequests).hasValue(KafkaConsoleClient.MAX_CONNECTIONS);

        // What the container does once the global timeout elapses.
        timeoutHandlers.forEach(entry -> entry.getValue().handleTimeout(entry.getKey()));

        for (CompletableFuture<Object> response : responses) {
            assertThat(((Response) response.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)).getStatus()).isEqualTo(504);
        }
        // The trickling exchanges gave their connections back, and the queued request was never sent.
        assertThat(proxyGet().getStatus()).isEqualTo(200);
        assertThat(trickledRequests).hasValue(KafkaConsoleClient.MAX_CONNECTIONS);
    }

    @Test
    void should_not_relay_the_browser_connection_headers_to_kafbat() throws Exception {
        Response response = (Response) proxy(
            "POST",
            "/api/echo",
            Map.of(
                "Connection",
                "close",
                "keep-alive",
                "timeout=5",
                "Upgrade",
                "websocket",
                "content-length",
                "999",
                "authorization",
                "Bearer browser-token",
                "Cookie",
                "Auth-Graviteeio-APIM=apim-session",
                "X-Custom",
                "kept"
            ),
            Buffer.buffer("hello")
        );

        String received = (String) response.getEntity();
        assertThat(received)
            .contains("x-custom=kept", "content-length=5", "body=hello")
            .doesNotContain("connection=", "keep-alive=", "upgrade=", "Bearer browser-token", "cookie=", "apim-session");
    }

    @Test
    void should_proxy_sub_resources_through_the_same_client() throws Exception {
        assertThat(((Response) proxy("SUB_GET", "/api/clusters/any/topics", Map.of(), null)).getStatus()).isEqualTo(200);
        assertThat(((Response) proxy("SUB_POST", "/api/echo", Map.of(), Buffer.buffer("payload"))).getEntity())
            .asString()
            .contains("body=payload");

        assertThat(registeredHttpClients()).isEqualTo(1);
    }

    @Test
    void should_close_its_clients_when_the_context_stops() throws Exception {
        assertThat(proxyGet().getStatus()).isEqualTo(200);
        HttpClient client = kafkaConsoleClient.client("localhost", server.actualPort(), false);

        kafkaConsoleClient.close();

        // A closed client refuses new requests; the next proxied request gets a fresh one.
        assertThatThrownBy(() -> client.request(io.vertx.core.http.HttpMethod.GET, "/"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("closed");
        assertThat(proxyGet().getStatus()).isEqualTo(200);
    }

    private Response proxyGet() throws Exception {
        return (Response) proxy("/api/clusters");
    }

    private Object proxy(String path) throws Exception {
        return proxy("GET", path, Map.of(), null);
    }

    private Object proxy(String method, String path, Map<String, String> headers, Buffer body) throws Exception {
        return proxyAsync(method, path, headers, body).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private CompletableFuture<Object> proxyAsync(String method, String path, Map<String, String> headers, Buffer body) {
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getRequestURI()).thenReturn("/management/v2/environments/DEFAULT/proxy-kafka-console" + path);
        when(httpRequest.getHeaderNames()).thenReturn(Collections.enumeration(headers.keySet()));
        headers.forEach((name, value) -> when(httpRequest.getHeader(name)).thenReturn(value));

        CompletableFuture<Object> resumed = new CompletableFuture<>();
        AsyncResponse asyncResponse = mock(AsyncResponse.class);
        doAnswer(invocation -> resumed.complete(invocation.getArgument(0)))
            .when(asyncResponse)
            .resume(any(Object.class));
        // A failure resumes through the Throwable overload.
        doAnswer(invocation -> resumed.complete(invocation.getArgument(0)))
            .when(asyncResponse)
            .resume(any(Throwable.class));
        when(asyncResponse.isDone()).thenAnswer(invocation -> resumed.isDone());
        doAnswer(invocation -> timeoutHandlers.add(Map.entry(asyncResponse, invocation.getArgument(0))))
            .when(asyncResponse)
            .setTimeoutHandler(any(TimeoutHandler.class));

        switch (method) {
            case "POST" -> cut.proxyKafkaConsolePost(asyncResponse, httpRequest, body);
            case "SUB_POST" -> cut.proxyKafkaConsolePostSubResource(asyncResponse, httpRequest, body);
            case "SUB_GET" -> cut.proxyKafkaConsoleGetSubResource(asyncResponse, httpRequest);
            default -> cut.proxyKafkaConsoleGet(asyncResponse, httpRequest);
        }

        return resumed;
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
