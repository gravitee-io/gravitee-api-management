/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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
package io.gravitee.apim.integration.tests.messages.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import com.graviteesource.entrypoint.websocket.WebSocketEntrypointConnectorFactory;
import com.rabbitmq.client.Delivery;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.integration.tests.messages.AbstractRabbitMQEndpointIntegrationTest;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.vertx.rxjava3.core.http.HttpClient;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

@GatewayTest
class WebsocketEntrypointRabbitMQEndpointIntegrationTest extends AbstractRabbitMQEndpointIntegrationTest {

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("websocket", EntrypointBuilder.build("websocket", WebSocketEntrypointConnectorFactory.class));
    }

    @Test
    @DeployApi({ "/apis/v4/messages/websocket/websocket-entrypoint-rabbitmq-endpoint-subscriber-none.json" })
    void should_receive_all_messages_with_none_qos(HttpClient httpClient) {
        httpClient
            .rxWebSocket("/test-none")
            .flatMapPublisher(websocket ->
                websocket.toFlowable().mergeWith(publishToRabbitMQ(exchange, routingKey, List.of("message"), 500).toFlowable())
            )
            .test()
            .awaitCount(1)
            .assertValue(frame -> {
                assertThat(frame).hasToString("message");
                return true;
            })
            .assertNoErrors();
    }

    @Test
    @DeployApi({ "/apis/v4/messages/websocket/websocket-entrypoint-rabbitmq-endpoint-subscriber.json" })
    void should_receive_all_messages_with_auto_qos(HttpClient httpClient) {
        httpClient
            .rxWebSocket("/test-auto")
            .flatMapPublisher(websocket ->
                websocket.toFlowable().mergeWith(publishToRabbitMQ(exchange, routingKey, List.of("message"), 1000).toFlowable())
            )
            .test()
            .awaitCount(1)
            .assertValue(frame -> {
                assertThat(frame).hasToString("message");
                return true;
            })
            .assertNoErrors();
    }

    @Test
    @DeployApi({ "/apis/v4/messages/websocket/websocket-entrypoint-rabbitmq-endpoint-publisher.json" })
    void should_publish_messages(HttpClient httpClient) {
        TestSubscriber<Delivery> testSubscriber = subscribeToRabbitMQ(exchange, routingKey).test();

        httpClient
            .rxWebSocket("/test")
            .flatMapCompletable(webSocket -> webSocket.writeFinalTextFrame("message pub"))
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertComplete()
            .assertNoErrors();

        testSubscriber
            .awaitCount(1)
            .assertValue(frame -> {
                assertThat(Buffer.buffer(frame.getBody())).hasToString("message pub");
                return true;
            });
    }

    @Test
    @DeployApi({ "/apis/v4/messages/websocket/websocket-entrypoint-rabbitmq-endpoint-publisher-subscriber.json" })
    void should_received_published_messages(HttpClient httpClient) {
        httpClient
            .rxWebSocket("/test")
            .flatMapPublisher(websocket -> websocket.writeTextMessage("message").toFlowable().mergeWith(websocket.toFlowable()))
            .test()
            .awaitCount(1)
            .assertValue(frame -> {
                assertThat(frame).hasToString("message");
                return true;
            })
            .assertNoErrors();
    }
}
