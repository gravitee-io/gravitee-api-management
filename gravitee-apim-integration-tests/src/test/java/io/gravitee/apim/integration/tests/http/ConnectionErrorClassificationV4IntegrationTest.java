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
package io.gravitee.apim.integration.tests.http;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.http.Fault;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.parameters.GatewayDynamicConfig;
import io.gravitee.apim.gateway.tests.sdk.reporter.FakeReporter;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.net.NetClientOptions;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * End-to-end coverage for the connection-error classification carved out by APIM-12769. For every error key
 * we induce the <em>real</em> failure (no mocked exceptions) and assert the reported {@link Metrics} carries
 * the expected status, error key and message — proving both halves of the classifier are actually reachable
 * with the exception types/messages we assumed.
 * <p>
 * The classifier mapping itself (which exception/message maps to which key) is exhaustively unit-tested in
 * {@code HttpProxyEndpointConnectorTest} and {@code HttpProtocolVerticleClassifyTest}; these integration tests
 * validate the runtime assumptions those unit tests cannot (which callback fires, what message the stack emits).
 *
 * @author GraviteeSource Team
 */
class ConnectionErrorClassificationV4IntegrationTest {

    private static final String API_PATH = "/test";
    private static final String ENDPOINT_PATH = "/endpoint";

    private static void configureProxyEntrypoint(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("http-proxy", EntrypointBuilder.build("http-proxy", HttpProxyEntrypointConnectorFactory.class));
    }

    private static void configureProxyEndpoint(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
        endpoints.putIfAbsent("http-proxy", EndpointBuilder.build("http-proxy", HttpProxyEndpointConnectorFactory.class));
    }

    private static void enableAnalytics(ReactableApi<?> api) {
        if (api.getDefinition() instanceof Api definition) {
            var analytics = new Analytics();
            analytics.setEnabled(true);
            definition.setAnalytics(analytics);
        }
    }

    private static BehaviorSubject<Metrics> captureReportedMetrics(FakeReporter fakeReporter) {
        BehaviorSubject<Metrics> subject = BehaviorSubject.create();
        fakeReporter.setReportableHandler(reportable -> {
            if (reportable instanceof Metrics metrics) {
                subject.onNext(metrics.toBuilder().build());
            }
        });
        return subject;
    }

    /**
     * Wait for the first reported {@link Metrics} carrying an error key and assert its status, key and (a
     * substring of) its message. Failing assertions surface the <em>actual</em> key/message so a wrong
     * runtime assumption is immediately visible.
     */
    private static void assertReportedError(
        BehaviorSubject<Metrics> metricsSubject,
        int expectedStatus,
        String expectedKey,
        String expectedMessageContains
    ) {
        metricsSubject
            .filter(metrics -> metrics.getErrorKey() != null)
            .take(1)
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValue(metrics -> {
                assertThat(metrics.getStatus()).isEqualTo(expectedStatus);
                assertThat(metrics.getErrorKey()).isEqualTo(expectedKey);
                // The actionable message is carried on the V4 Diagnostic (metrics.failure), which both the
                // connector's interruptWith path and the connection-handler decoration populate. The legacy
                // flat errorMessage is only set on some paths, so we assert against the Diagnostic.
                assertThat(metrics.getFailure()).as("failure diagnostic").isNotNull();
                assertThat(metrics.getFailure().getKey()).isEqualTo(expectedKey);
                assertThat(metrics.getFailure().getMessage()).contains(expectedMessageContains);
                return true;
            });
    }

    private static String targetConfiguration(String target) {
        ObjectNode configuration = new ObjectMapper().createObjectNode();
        configuration.put("target", target);
        return configuration.toString();
    }

    // -----------------------------------------------------------------------------------------------------
    // Upstream (gateway-as-client) failures surfaced through the endpoint connector.
    // -----------------------------------------------------------------------------------------------------

    @Nested
    @GatewayTest
    @DeployApi("/apis/v4/http/api.json")
    class UpstreamFaults extends AbstractGatewayTest {

        private BehaviorSubject<Metrics> metricsSubject;

        @BeforeEach
        void setUp() {
            metricsSubject = captureReportedMetrics(getBean(FakeReporter.class));
        }

        @Override
        public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
            configureProxyEntrypoint(entrypoints);
        }

        @Override
        public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
            configureProxyEndpoint(endpoints);
        }

        @Override
        public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
            enableAnalytics(api);
        }

        @Test
        @DisplayName("Upstream resets the connection -> 502 UPSTREAM_CONNECTION_RESET")
        void should_classify_upstream_connection_reset(HttpClient httpClient) {
            wiremock.stubFor(get(urlPathEqualTo(ENDPOINT_PATH)).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

            sendGet(httpClient);

            assertReportedError(metricsSubject, 502, "UPSTREAM_CONNECTION_RESET", "The upstream reset the connection");
        }

        @Test
        @DisplayName("Upstream closes the connection without responding -> 502 UPSTREAM_CONNECTION_CLOSED")
        void should_classify_upstream_connection_closed(HttpClient httpClient) {
            wiremock.stubFor(get(urlPathEqualTo(ENDPOINT_PATH)).willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));

            sendGet(httpClient);

            assertReportedError(metricsSubject, 502, "UPSTREAM_CONNECTION_CLOSED", "The upstream closed the connection");
        }
    }

    @Nested
    @GatewayTest
    @DeployApi("/apis/v4/http/api.json")
    class UpstreamDnsFailure extends AbstractGatewayTest {

        private BehaviorSubject<Metrics> metricsSubject;

        @BeforeEach
        void setUp() {
            metricsSubject = captureReportedMetrics(getBean(FakeReporter.class));
        }

        @Override
        public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
            configureProxyEntrypoint(entrypoints);
        }

        @Override
        public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
            configureProxyEndpoint(endpoints);
        }

        @Override
        public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
            enableAnalytics(api);
            // Host that cannot resolve. The ".invalid" TLD is reserved by RFC 2606 and never resolves.
            // The port is irrelevant: DNS resolution fails first. Not the localhost:8080 placeholder, so the
            // SDK leaves it untouched.
            if (api.getDefinition() instanceof Api definition) {
                updateEndpoints(definition, endpoint ->
                    endpoint.setConfiguration(targetConfiguration("http://gravitee-upstream-does-not-exist.invalid:8080" + ENDPOINT_PATH))
                );
            }
        }

        @Test
        @DisplayName("Upstream host does not resolve -> 502 UPSTREAM_DNS_FAILURE")
        void should_classify_dns_failure(HttpClient httpClient) {
            sendGet(httpClient);

            assertReportedError(metricsSubject, 502, "UPSTREAM_DNS_FAILURE", "Unable to resolve the upstream host");
        }
    }

    @Nested
    @GatewayTest
    @DeployApi("/apis/v4/http/api.json")
    class UpstreamConnectionRefused extends AbstractGatewayTest {

        private BehaviorSubject<Metrics> metricsSubject;

        @BeforeEach
        void setUp() {
            metricsSubject = captureReportedMetrics(getBean(FakeReporter.class));
        }

        @Override
        public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
            configureProxyEntrypoint(entrypoints);
        }

        @Override
        public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
            configureProxyEndpoint(endpoints);
        }

        @Override
        public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
            enableAnalytics(api);
            // Port 1 on loopback is (effectively always) closed -> immediate TCP connection refused. Not the
            // localhost:8080 placeholder, so the SDK leaves it untouched.
            if (api.getDefinition() instanceof Api definition) {
                updateEndpoints(definition, endpoint ->
                    endpoint.setConfiguration(targetConfiguration("http://127.0.0.1:1" + ENDPOINT_PATH))
                );
            }
        }

        @Test
        @DisplayName("Upstream refuses the connection -> 502 UPSTREAM_CONNECTION_REFUSED")
        void should_classify_connection_refused(HttpClient httpClient) {
            sendGet(httpClient);

            assertReportedError(metricsSubject, 502, "UPSTREAM_CONNECTION_REFUSED", "The upstream refused the connection");
        }
    }

    @Nested
    @GatewayTest
    @DeployApi("/apis/v4/http/api.json")
    class UpstreamIdleTimeout extends AbstractGatewayTest {

        private static final int HANG_BACKEND_PORT = 19998;

        private BehaviorSubject<Metrics> metricsSubject;

        @BeforeEach
        void setUp() {
            metricsSubject = captureReportedMetrics(getBean(FakeReporter.class));
        }

        @Override
        public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
            configureProxyEntrypoint(entrypoints);
        }

        @Override
        public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
            configureProxyEndpoint(endpoints);
        }

        @Override
        public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
            enableAnalytics(api);
            // Point at the silent backend (below); short idleTimeout, longer readTimeout so idle fires first.
            // A WireMock delayed response does not reproduce a Vert.x connection-idle — only a truly silent
            // socket does — so we use a raw hang backend rather than WireMock here.
            if (api.getDefinition() instanceof Api definition) {
                updateEndpoints(definition, endpoint -> {
                    endpoint.setConfiguration(targetConfiguration("http://localhost:" + HANG_BACKEND_PORT + ENDPOINT_PATH));
                    endpoint.setSharedConfigurationOverride(
                        "{\"http\":{\"idleTimeout\":2000,\"readTimeout\":8000,\"connectTimeout\":3000}}"
                    );
                });
            }
        }

        @Test
        @DisplayName("Endpoint idle timeout closes the upstream connection -> 504 UPSTREAM_IDLE_TIMEOUT")
        void should_classify_upstream_idle_timeout(HttpClient httpClient) throws Exception {
            try (ServerSocket hangBackend = new ServerSocket(HANG_BACKEND_PORT)) {
                List<Socket> held = new ArrayList<>();
                Thread acceptor = new Thread(() -> {
                    try {
                        while (!hangBackend.isClosed()) {
                            held.add(hangBackend.accept()); // accept and hold the connection; never respond
                        }
                    } catch (IOException ignored) {
                        // server socket closed at end of test
                    }
                });
                acceptor.setDaemon(true);
                acceptor.start();

                sendGet(httpClient);

                assertReportedError(metricsSubject, 504, "UPSTREAM_IDLE_TIMEOUT", "idle timeout");

                for (Socket socket : held) {
                    socket.close();
                }
            }
        }
    }

    // -----------------------------------------------------------------------------------------------------
    // Server (gateway-as-server) client-abort failures surfaced through the connection-level handlers.
    // -----------------------------------------------------------------------------------------------------

    @Nested
    @GatewayTest
    @DeployApi("/apis/v4/http/api.json")
    class ClientTcpReset extends AbstractGatewayTest {

        private BehaviorSubject<Metrics> metricsSubject;

        @BeforeEach
        void setUp() {
            metricsSubject = captureReportedMetrics(getBean(FakeReporter.class));
        }

        @Override
        public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
            configureProxyEntrypoint(entrypoints);
        }

        @Override
        public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
            configureProxyEndpoint(endpoints);
        }

        @Override
        public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
            enableAnalytics(api);
        }

        @Test
        @DisplayName("Client resets the TCP connection mid-request -> 499 CLIENT_ABORTED_TCP_RESET")
        void should_classify_client_tcp_reset(Vertx vertx, GatewayDynamicConfig.Config gatewayConfig) {
            // Backend is slow so the request is still in flight when the client sends a RST.
            wiremock.stubFor(get(urlPathEqualTo(ENDPOINT_PATH)).willReturn(ok("backend").withFixedDelay(5000)));

            int port = gatewayConfig.httpPort();
            // soLinger=0 -> close() emits a TCP RST instead of a graceful FIN, which the gateway sees as
            // "Connection reset by peer" on its connection exception handler.
            vertx
                .createNetClient(new NetClientOptions().setSoLinger(0))
                .rxConnect(port, "localhost")
                .subscribe(socket -> {
                    socket.write("GET " + API_PATH + " HTTP/1.1\r\nHost: localhost:" + port + "\r\n\r\n");
                    // Close (RST) once the request is dispatched and waiting on the slow backend.
                    vertx.getDelegate().setTimer(800, id -> socket.getDelegate().close());
                });

            assertReportedError(metricsSubject, 499, "CLIENT_ABORTED_TCP_RESET", "The client reset the connection");
        }
    }

    @Nested
    @GatewayTest
    @DeployApi("/apis/v4/http/api.json")
    class ClientIdleTimeout extends AbstractGatewayTest {

        private BehaviorSubject<Metrics> metricsSubject;

        @BeforeEach
        void setUp() {
            metricsSubject = captureReportedMetrics(getBean(FakeReporter.class));
        }

        @Override
        protected void configureGateway(GatewayConfigurationBuilder gatewayConfigurationBuilder) {
            super.configureGateway(gatewayConfigurationBuilder);
            // http.idleTimeout is expressed in SECONDS (Vert.x default unit; the node layer never overrides it).
            gatewayConfigurationBuilder.set("http.idleTimeout", 1);
        }

        @Override
        public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
            configureProxyEntrypoint(entrypoints);
        }

        @Override
        public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
            configureProxyEndpoint(endpoints);
        }

        @Override
        public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
            enableAnalytics(api);
        }

        @Test
        @DisplayName("Server idle timeout closes an in-flight connection -> 499 CLIENT_ABORTED_IDLE_TIMEOUT")
        void should_classify_idle_timeout(HttpClient httpClient) {
            // Backend slower than the 1s server idle timeout: the inbound connection sits idle while the
            // gateway waits, the server idle timeout fires and closes it -> the in-flight request is aborted.
            wiremock.stubFor(get(urlPathEqualTo(ENDPOINT_PATH)).willReturn(ok("backend").withFixedDelay(8000)));

            sendGet(httpClient);

            assertReportedError(metricsSubject, 499, "CLIENT_ABORTED_IDLE_TIMEOUT", "idle timeout");
        }
    }

    private static void sendGet(HttpClient httpClient) {
        // Fire the request; the gateway reports metrics independently of whether the client reads the body
        // (and for client-abort scenarios the send itself errors when the gateway closes the connection).
        httpClient.rxRequest(HttpMethod.GET, API_PATH).flatMap(HttpClientRequest::rxSend).ignoreElement().onErrorComplete().subscribe();
    }
}
