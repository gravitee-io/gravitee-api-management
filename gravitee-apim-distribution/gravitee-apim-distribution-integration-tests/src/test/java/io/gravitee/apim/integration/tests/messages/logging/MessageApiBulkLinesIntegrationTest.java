/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.apim.integration.tests.messages.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.graviteesource.entrypoint.sse.SseEntrypointConnectorFactory;
import com.graviteesource.reactor.message.MessageApiReactorFactory;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.reactor.ReactorBuilder;
import io.gravitee.apim.gateway.tests.sdk.reporter.FakeReporter;
import io.gravitee.apim.integration.tests.messages.logging.BulkLineRecorder.BulkLine;
import io.gravitee.apim.plugin.reactor.ReactorPlugin;
import io.gravitee.common.http.MediaType;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactory;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.node.api.Node;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.mock.MockEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@GatewayTest
class MessageApiBulkLinesIntegrationTest extends AbstractGatewayTest {

    private BulkLineRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder = new BulkLineRecorder(getBean(Node.class), getBean(FakeReporter.class));
    }

    @Test
    @DeployApi("/apis/v4/messages/metrics/sse-entrypoint-mock-endpoint.json")
    void should_write_the_opening_and_the_end_of_an_sse_connection_as_two_documents(HttpClient httpClient) {
        var sse = httpClient
            .rxRequest(HttpMethod.GET, "/test")
            .flatMap(request -> request.putHeader(HttpHeaderNames.ACCEPT.toString(), MediaType.TEXT_EVENT_STREAM).rxSend())
            .flatMapPublisher(response -> response.toFlowable())
            .test();

        await()
            .atMost(Duration.ofSeconds(30))
            .until(() -> recorder.lines("v4-message-metrics").size() >= 4);
        sse.cancel();
        await()
            .atMost(Duration.ofSeconds(30))
            .until(() -> recorder.lines("v4-metrics").size() >= 2 && recorder.lines("v4-log").size() >= 2);

        List<BulkLine> connection = recorder.lines("v4-metrics");
        assertThat(connection).hasSize(2);
        String requestId = connection.get(1).source().get("request-id").asText();
        assertThat(connection.get(0).id()).isEqualTo(requestId + ":open");
        assertThat(connection.get(0).source().get("connection-phase").asText()).isEqualTo("open");
        assertThat(connection.get(0).source().get("request-ended").asText()).isEqualTo("false");
        assertThat(connection.get(1).id()).isEqualTo(requestId);
        assertThat(connection.get(1).source().get("connection-phase").asText()).isEqualTo("end");
        assertThat(connection.get(1).source().get("request-ended").asText()).isEqualTo("true");
        assertThat(recorder.lines("v4-log")).extracting(BulkLine::id).containsExactly(requestId + ":open", requestId);
        assertThat(recorder.lines("v4-message-metrics")).extracting(BulkLine::id).doesNotHaveDuplicates();
    }

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("sse", EntrypointBuilder.build("sse", SseEntrypointConnectorFactory.class));
    }

    @Override
    public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
        endpoints.putIfAbsent("mock", EndpointBuilder.build("mock", MockEndpointConnectorFactory.class));
    }

    @Override
    public void configureReactors(Set<ReactorPlugin<? extends ReactorFactory<?>>> reactors) {
        reactors.add(ReactorBuilder.build(MessageApiReactorFactory.class));
    }

    @Override
    public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
        if (api.getDefinition() instanceof Api definition) {
            definition.setAnalytics(MessageAnalytics.fullLogging());
        }
    }
}
