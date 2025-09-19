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
package io.gravitee.apim.integration.tests.messages.sse;

import static org.assertj.core.api.Assertions.assertThat;

import com.graviteesource.entrypoint.sse.SseEntrypointConnectorFactory;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.integration.tests.messages.AbstractRabbitMQEndpointIntegrationTest;
import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.core.http.HttpClient;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

@GatewayTest
class SseEntrypointRabbitMQEndpointIntegrationTest extends AbstractRabbitMQEndpointIntegrationTest {

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("sse", EntrypointBuilder.build("sse", SseEntrypointConnectorFactory.class));
    }

    @Test
    @DeployApi({ "/apis/v4/messages/sse/sse-entrypoint-rabbitmq-endpoint.json" })
    void should_receive_messages(HttpClient httpClient) {
        final TestSubscriber<Buffer> obs = httpClient
            .rxRequest(HttpMethod.GET, "/test")
            .flatMap(request -> {
                request.putHeader(HttpHeaderNames.ACCEPT.toString(), MediaType.TEXT_EVENT_STREAM);
                return request.rxSend();
            })
            .flatMapPublisher(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return response
                    .toFlowable()
                    .mergeWith(publishToRabbitMQ(exchange, routingKey, List.of("message1", "message2", "message3")).toFlowable());
            })
            .test();

        // We expect 4 chunks, 1 retry message and 3 messages
        obs
            .awaitCount(4)
            .assertValueAt(0, chunk -> {
                SseAssertions.assertRetry(chunk);
                return true;
            })
            .assertValueAt(1, chunk -> {
                SseAssertions.assertOnMessage(chunk, "message1");
                return true;
            })
            .assertValueAt(2, chunk -> {
                SseAssertions.assertOnMessage(chunk, "message2");
                return true;
            })
            .assertValueAt(3, chunk -> {
                SseAssertions.assertOnMessage(chunk, "message3");
                return true;
            })
            .assertNoErrors();
    }
}
