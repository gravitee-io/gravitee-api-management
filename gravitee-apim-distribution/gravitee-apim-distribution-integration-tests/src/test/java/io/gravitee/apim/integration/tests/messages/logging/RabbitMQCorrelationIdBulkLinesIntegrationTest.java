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
package io.gravitee.apim.integration.tests.messages.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.graviteesource.entrypoint.sse.SseEntrypointConnectorFactory;
import com.rabbitmq.client.AMQP;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.reporter.FakeReporter;
import io.gravitee.apim.integration.tests.messages.AbstractRabbitMQEndpointIntegrationTest;
import io.gravitee.apim.integration.tests.messages.logging.BulkLineRecorder.BulkLine;
import io.gravitee.common.http.MediaType;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.node.api.Node;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.rabbitmq.OutboundMessage;

@GatewayTest
class RabbitMQCorrelationIdBulkLinesIntegrationTest extends AbstractRabbitMQEndpointIntegrationTest {

    private static final String SHARED_CORRELATION_ID = "order-42";

    private BulkLineRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder = new BulkLineRecorder(getBean(Node.class), getBean(FakeReporter.class));
    }

    @Test
    @DeployApi("/apis/v4/messages/sse/sse-entrypoint-rabbitmq-endpoint.json")
    void should_give_an_id_of_its_own_to_each_message_sharing_a_broker_correlation_id(HttpClient httpClient) {
        var properties = new AMQP.BasicProperties.Builder().correlationId(SHARED_CORRELATION_ID).build();
        var messages = Flux.just("message1", "message2", "message3").map(m ->
            new OutboundMessage(exchange, routingKey, properties, m.getBytes())
        );

        var sse = httpClient
            .rxRequest(HttpMethod.GET, "/test")
            .flatMap(request -> request.putHeader(HttpHeaderNames.ACCEPT.toString(), MediaType.TEXT_EVENT_STREAM).rxSend())
            .flatMapPublisher(response -> response.toFlowable().mergeWith(publishToRabbitMQ(messages, 500).toFlowable()))
            .test();

        await()
            .atMost(Duration.ofSeconds(30))
            .until(() -> recorder.lines("v4-message-metrics").size() >= 6 && recorder.lines("v4-message-log").size() >= 6);
        sse.cancel();

        List<BulkLine> messageMetrics = recorder.lines("v4-message-metrics");
        assertThat(messageMetrics).hasSize(6);
        assertThat(messageMetrics).extracting(BulkLine::id).doesNotHaveDuplicates();
        assertThat(recorder.lines("v4-message-log")).hasSize(6).extracting(BulkLine::id).doesNotHaveDuplicates();
        assertThat(messageMetrics)
            .extracting(line -> line.source().get("correlation-id").asText())
            .containsOnly(SHARED_CORRELATION_ID);
    }

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("sse", EntrypointBuilder.build("sse", SseEntrypointConnectorFactory.class));
    }

    @Override
    public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
        super.configureApi(api, definitionClass);
        if (api.getDefinition() instanceof Api definition) {
            definition.setAnalytics(MessageAnalytics.fullLogging());
        }
    }
}
