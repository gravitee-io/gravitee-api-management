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
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.http.Fault;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.reporter.FakeReporter;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import io.vertx.rxjava3.core.http.HttpClientResponse;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * End-to-end coverage that gateway→backend (upstream) connection failures of a v4 HTTP proxy API are translated into
 * the expected HTTP status and the fine-grained error key / diagnostic recorded on the metrics.
 */
@GatewayTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ConnectionFailureV4IntegrationTest extends AbstractGatewayTest {

    private static final String TINY_POOL_API_ID = "my-api-v4-tiny-pool";

    private BehaviorSubject<Metrics> metricsSubject;
    private int tinyPoolBackendPort;

    @BeforeEach
    void setUpMetrics() {
        metricsSubject = BehaviorSubject.create();
        FakeReporter fakeReporter = getBean(FakeReporter.class);
        fakeReporter.setReportableHandler(reportable -> {
            if (reportable instanceof Metrics metrics) {
                metricsSubject.onNext(metrics.toBuilder().build());
            }
        });
    }

    @Override
    public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
        if (api.getDefinition() instanceof Api definition) {
            var analytics = new Analytics();
            analytics.setEnabled(true);
            definition.setAnalytics(analytics);
            if (TINY_POOL_API_ID.equals(definition.getId())) {
                // The silent backend is a raw socket opened by the test itself: point the endpoint at a random
                // free port instead of a fixed one so parallel CI runs cannot collide.
                tinyPoolBackendPort = getAvailablePort();
                updateEndpointsPort(definition, tinyPoolBackendPort);
            }
        }
    }

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("http-proxy", EntrypointBuilder.build("http-proxy", HttpProxyEntrypointConnectorFactory.class));
    }

    @Override
    public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
        endpoints.putIfAbsent("http-proxy", EndpointBuilder.build("http-proxy", HttpProxyEndpointConnectorFactory.class));
    }

    @Test
    @DeployApi("/apis/v4/http/connectionfailure/api-refused-with-umbrella-template.json")
    void should_fall_back_to_the_umbrella_response_template_for_a_fine_grained_key(HttpClient httpClient) {
        // The connection is refused → fine key GATEWAY_CLIENT_CONNECTION_REFUSED, parent GATEWAY_CLIENT_CONNECTION_ERROR.
        // No template targets the fine key, so the parentErrorKey carried on the ExecutionFailure must drive the
        // umbrella template — an end-to-end check of the cross-module parentErrorKey contract between the http-proxy
        // classifier and ResponseTemplateBasedFailureProcessor.
        // The umbrella template status (503) is deliberately different from the connection-refused default (502), so a
        // 503 here proves the template — resolved via the parent fallback — actually drove the response status.
        httpClient
            .rxRequest(HttpMethod.GET, "/test")
            .flatMap(HttpClientRequest::rxSend)
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(503);
                return response.rxBody();
            })
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValue(body -> {
                assertThat(body.toString()).isEqualTo("umbrella template fired");
                return true;
            });
    }

    @Test
    @DeployApi("/apis/v4/http/api.json")
    void should_report_connection_reset_when_backend_resets_the_connection(HttpClient httpClient) {
        wiremock.stubFor(get("/endpoint").willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        httpClient
            .rxRequest(HttpMethod.GET, "/test")
            .flatMap(HttpClientRequest::rxSend)
            .flatMapPublisher(response -> {
                assertThat(response.statusCode()).isEqualTo(502);
                return response.toFlowable();
            })
            .ignoreElements()
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertComplete();

        metricsSubject
            .filter(metrics -> metrics.getErrorKey() != null)
            .take(1)
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValue(metrics -> {
                assertThat(metrics.getStatus()).isEqualTo(502);
                assertThat(metrics.getErrorKey()).isEqualTo("GATEWAY_CLIENT_CONNECTION_RESET");
                assertThat(metrics.getFailure()).isNotNull();
                assertThat(metrics.getFailure().getKey()).isEqualTo("GATEWAY_CLIENT_CONNECTION_RESET");
                assertThat(metrics.getFailure().getComponentType()).isEqualTo("ENDPOINT");
                return true;
            });
    }

    @Test
    @DeployApi("/apis/v4/http/connectionfailure/api-tiny-pool.json")
    void should_report_connect_timeout_when_request_times_out_before_acquiring_a_connection(HttpClient httpClient) throws Exception {
        // Single-connection pool + silent backend: the first request occupies the only upstream connection and times
        // out reading the backend (READ_TIMEOUT), while the others time out QUEUED for a connection — they never
        // reached the backend, so they are CONNECT_TIMEOUT, not a misleading backend read timeout. The distinction is
        // made from whether the connection was acquired, never from the timeout message wording.
        try (ServerSocket silentBackend = new ServerSocket(tinyPoolBackendPort)) {
            List<Socket> held = Collections.synchronizedList(new ArrayList<>());
            Thread acceptor = new Thread(() -> {
                try {
                    while (!silentBackend.isClosed()) {
                        held.add(silentBackend.accept()); // accept and hold; never respond
                    }
                } catch (IOException ignored) {
                    // server socket closed at end of test
                }
            });
            acceptor.setDaemon(true);
            acceptor.start();

            // Subscribe BEFORE firing the requests: the metrics are reported while the requests are still being
            // awaited, and the BehaviorSubject only replays the latest one to a late subscriber. Both keys are 504, so
            // status alone can't tell them apart — assert each key explicitly to prove the connect-vs-read split.
            var connectTimeoutMetrics = metricsSubject
                .filter(metrics -> "GATEWAY_CLIENT_CONNECT_TIMEOUT".equals(metrics.getErrorKey()))
                .take(1)
                .test();
            var readTimeoutMetrics = metricsSubject
                .filter(metrics -> "GATEWAY_CLIENT_READ_TIMEOUT".equals(metrics.getErrorKey()))
                .take(1)
                .test();

            Flowable.range(0, 3)
                .flatMapSingle(i -> httpClient.rxRequest(HttpMethod.GET, "/test").flatMap(HttpClientRequest::rxSend))
                .map(HttpClientResponse::statusCode)
                .toList()
                .test()
                .awaitDone(30, TimeUnit.SECONDS)
                .assertValue(statuses -> statuses.stream().allMatch(status -> status == 504));

            // The queued requests never acquired a connection → CONNECT_TIMEOUT.
            connectTimeoutMetrics
                .awaitDone(30, TimeUnit.SECONDS)
                .assertValue(metrics -> {
                    assertThat(metrics.getStatus()).isEqualTo(504);
                    assertThat(metrics.getFailure()).isNotNull();
                    assertThat(metrics.getFailure().getKey()).isEqualTo("GATEWAY_CLIENT_CONNECT_TIMEOUT");
                    assertThat(metrics.getFailure().getComponentType()).isEqualTo("ENDPOINT");
                    return true;
                });

            // The request that held the single connection read-timed-out on the silent backend → READ_TIMEOUT.
            readTimeoutMetrics
                .awaitDone(30, TimeUnit.SECONDS)
                .assertValue(metrics -> {
                    assertThat(metrics.getStatus()).isEqualTo(504);
                    assertThat(metrics.getFailure()).isNotNull();
                    assertThat(metrics.getFailure().getKey()).isEqualTo("GATEWAY_CLIENT_READ_TIMEOUT");
                    return true;
                });

            synchronized (held) {
                for (Socket socket : held) {
                    socket.close();
                }
            }
        }
    }

    @Test
    @DeployApi("/apis/v4/http/connectionfailure/api-dns-failure.json")
    void should_report_dns_resolution_error_when_backend_host_is_unresolvable(HttpClient httpClient) {
        httpClient
            .rxRequest(HttpMethod.GET, "/test")
            .flatMap(HttpClientRequest::rxSend)
            .flatMapPublisher(response -> {
                assertThat(response.statusCode()).isEqualTo(502);
                return response.toFlowable();
            })
            .ignoreElements()
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertComplete();

        metricsSubject
            .filter(metrics -> metrics.getErrorKey() != null)
            .take(1)
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValue(metrics -> {
                assertThat(metrics.getStatus()).isEqualTo(502);
                assertThat(metrics.getErrorKey()).isEqualTo("GATEWAY_CLIENT_DNS_RESOLUTION_ERROR");
                assertThat(metrics.getFailure()).isNotNull();
                assertThat(metrics.getFailure().getComponentType()).isEqualTo("ENDPOINT");
                return true;
            });
    }
}
