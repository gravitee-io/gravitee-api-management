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
package io.gravitee.apim.integration.tests.tracing;

import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static io.gravitee.apim.integration.tests.messages.sse.SseAssertions.assertOnMessage;
import static io.gravitee.apim.integration.tests.messages.sse.SseAssertions.assertRetry;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import com.graviteesource.entrypoint.sse.SseEntrypointConnectorFactory;
import com.graviteesource.reactor.message.MessageApiReactorFactory;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.apim.gateway.tests.sdk.reactor.ReactorBuilder;
import io.gravitee.apim.integration.tests.fake.LatencyPolicy;
import io.gravitee.apim.integration.tests.fake.MessageFlowReadyPolicy;
import io.gravitee.apim.plugin.reactor.ReactorPlugin;
import io.gravitee.common.http.MediaType;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.analytics.tracing.Tracing;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactory;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.endpoint.mock.MockEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.core.http.HttpClient;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
@GatewayTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class OpenTelemetryTracingV4IntegrationTest extends AbstractGatewayTest {

    private static final JaegerTestContainer container = new JaegerTestContainer();

    public static final String MESSAGE = "{ \"message\": \"hello\" }";

    @AfterAll
    static void stop() {
        container.stop();
    }

    @Override
    protected void configureGateway(final GatewayConfigurationBuilder gatewayConfigurationBuilder) {
        container.start();
        gatewayConfigurationBuilder
            .set("services.opentelemetry.enabled", "true")
            .set("services.opentelemetry.exporter.endpoint", "http://localhost:" + container.getCollectorGrpcPort())
            .set("services.opentelemetry.exporter.protocol", "grpc");
    }

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("sse", EntrypointBuilder.build("sse", SseEntrypointConnectorFactory.class));
        entrypoints.putIfAbsent("http-proxy", EntrypointBuilder.build("http-proxy", HttpProxyEntrypointConnectorFactory.class));
    }

    @Override
    public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
        endpoints.putIfAbsent("http-proxy", EndpointBuilder.build("http-proxy", HttpProxyEndpointConnectorFactory.class));
        endpoints.putIfAbsent("mock", EndpointBuilder.build("mock", MockEndpointConnectorFactory.class));
    }

    @Override
    public void configureReactors(Set<ReactorPlugin<? extends ReactorFactory<?>>> reactors) {
        reactors.add(ReactorBuilder.build(MessageApiReactorFactory.class));
    }

    @Override
    public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
        final Tracing tracing = new Tracing();
        tracing.setEnabled(true);

        var analytics = new Analytics();
        analytics.setEnabled(true);
        analytics.setTracing(tracing);

        if (api.getDefinition() instanceof Api) {
            ((Api) api.getDefinition()).setAnalytics(analytics);
        }
    }

    @Override
    public void configurePolicies(Map<String, PolicyPlugin> policies) {
        policies.put("latency", PolicyBuilder.build("latency", LatencyPolicy.class, LatencyPolicy.LatencyConfiguration.class));
        policies.put("message-flow-ready", PolicyBuilder.build("message-flow-ready", MessageFlowReadyPolicy.class));
    }

    @Test
    @DeployApi("/apis/v4/http/api-tracing.json")
    void should_trace_request_on_http_api(HttpClient httpClient) throws Exception {
        wiremock.stubFor(post("/endpoint").willReturn(ok("response from backend")));

        httpClient
            .rxRequest(HttpMethod.POST, "/test")
            .flatMap(request -> request.rxSend("body request"))
            .flatMapPublisher(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return response.toFlowable();
            })
            .test()
            .await()
            .assertComplete()
            .assertValue(body -> {
                assertThat(body.toString()).isEqualTo("response from backend");
                return true;
            })
            .assertNoErrors();

        Set<String> expectedOperationNames = Set.of(
            "POST",
            "Request phase",
            "REQUEST Processor (processor-metrics)",
            "REQUEST Processor (pre-processor-transaction)",
            "REQUEST Processor (processor-x-forward-for)",
            "REQUEST flow (api-flow-1)",
            "REQUEST policy-latency",
            "REQUEST Security (keyless)",
            "endpoint-invoker",
            "POST /endpoint",
            "Response phase",
            "RESPONSE flow (api-flow-1)",
            "RESPONSE policy-latency",
            "RESPONSE Processor (processor-reporter)",
            "RESPONSE Processor (processor-response-time)",
            "REQUEST Processor (processor-connection-drain)"
        );

        await()
            .atMost(30, SECONDS)
            .untilAsserted(() -> {
                var client = container.client(vertx.getDelegate());
                var response = client
                    .get("/api/traces")
                    .addQueryParam("service", "my-api-v4")
                    .send()
                    .toCompletionStage()
                    .toCompletableFuture()
                    .get();

                assertThat(response.statusCode()).isEqualTo(200);
                assertData(response.bodyAsJsonObject(), expectedOperationNames);
            });
    }

    @Test
    @DeployApi("/apis/v4/messages/sse/sse-entrypoint-mock-endpoint-tracing.json")
    void should_get_messages_with_default_configuration(HttpClient httpClient) {
        startSseStream(httpClient)
            // expect 3 chunks: retry, two messages
            .awaitCount(3)
            .assertValueAt(0, chunk -> {
                assertRetry(chunk);
                return true;
            })
            .assertValueAt(1, chunk -> {
                assertOnMessage(chunk, 0L, MESSAGE);
                return true;
            })
            .assertValueAt(2, chunk -> {
                assertOnMessage(chunk, 1L, MESSAGE);
                return true;
            })
            .cancel();

        httpClient.close().blockingAwait();

        Set<String> expectedOperationNames = Set.of(
            "GET /test",
            "REQUEST Processor (processor-metrics)",
            "REQUEST Processor (pre-processor-transaction)",
            "REQUEST Processor (processor-x-forward-for)",
            "REQUEST Security (keyless)",
            "Request phase",
            "REQUEST flow (api-flow ready)",
            "MESSAGE_REQUEST flow (api-flow ready)",
            "endpoint-invoker",
            "Response phase",
            "RESPONSE flow (api-flow ready)",
            "MESSAGE_RESPONSE flow (api-flow ready)",
            "Subscribe",
            "message (0)",
            "policy-message-flow-ready",
            "RESPONSE Processor (processor-response-time)",
            "RESPONSE Processor (processor-reporter)",
            "REQUEST Processor (processor-connection-drain)"
        );
        await()
            .atMost(30, SECONDS)
            .untilAsserted(() -> {
                var client = container.client(vertx.getDelegate());
                var response = client
                    .get("/api/traces")
                    .addQueryParam("service", "my-api")
                    .send()
                    .toCompletionStage()
                    .toCompletableFuture()
                    .get();

                assertThat(response.statusCode()).isEqualTo(200);
                assertData(response.bodyAsJsonObject(), expectedOperationNames);
            });
    }

    private static TestSubscriber<Buffer> startSseStream(HttpClient httpClient) {
        return httpClient
            .rxRequest(HttpMethod.GET, "/test")
            .flatMap(request -> {
                request.putHeader(HttpHeaderNames.ACCEPT.toString(), MediaType.TEXT_EVENT_STREAM);
                return request.rxSend();
            })
            .flatMapPublisher(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return response.toFlowable();
            })
            .test();
    }

    private void assertData(JsonObject json, Set<String> expectedOperationName) {
        var data = json.getJsonArray("data");
        assertThat(data).isNotEmpty();

        var trace = data.getJsonObject(0);
        var spans = trace.getJsonArray("spans");

        String[] operationNames = spans
            .stream()
            .map(JsonObject.class::cast)
            .map(span -> span.getString("operationName"))
            .toArray(String[]::new);
        assertThat(expectedOperationName).containsOnly(operationNames);
    }
}
