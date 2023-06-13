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
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.integration.tests.messages.AbstractMqtt5EndpointIntegrationTest;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.reactive.api.qos.Qos;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.kafka.client.consumer.KafkaConsumer;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@GatewayTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class WebsocketEntrypointMqtt5EndpointIntegrationTest extends AbstractMqtt5EndpointIntegrationTest {

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("websocket", EntrypointBuilder.build("websocket", WebSocketEntrypointConnectorFactory.class));
    }

    @EnumSource(value = Qos.class, names = { "NONE", "AUTO" })
    @ParameterizedTest(name = "should receive all messages with {0} qos")
    @DeployApi(
        {
            "/apis/v4/messages/websocket/websocket-entrypoint-mqtt5-endpoint-subscriber-none.json",
            "/apis/v4/messages/websocket/websocket-entrypoint-mqtt5-endpoint-subscriber.json",
        }
    )
    void should_receive_all_messages_with_qos(Qos qos, HttpClient httpClient) {
        httpClient
            .rxWebSocket("/test-" + qos.getLabel())
            .flatMapPublisher(websocket ->
                websocket.toFlowable().zipWith(publishToMqtt5(mqtt5RxClient, TEST_TOPIC, "message", true), (w, m) -> w)
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
    @DeployApi({ "/apis/v4/messages/websocket/websocket-entrypoint-mqtt5-endpoint-publisher.json" })
    void should_publish_messages(HttpClient httpClient) {
        TestSubscriber<Mqtt5Publish> testSubscriber = subscribeToMqtt5Flowable(mqtt5RxClient, TEST_TOPIC).test();

        httpClient
            .rxWebSocket("/test")
            .flatMapCompletable(webSocket -> webSocket.writeFinalTextFrame("message"))
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertComplete()
            .assertNoErrors();

        testSubscriber
            .awaitCount(1)
            .assertValue(frame -> {
                assertThat(Buffer.buffer(frame.getPayloadAsBytes())).hasToString("message");
                return true;
            });
    }

    @Test
    @DeployApi({ "/apis/v4/messages/websocket/websocket-entrypoint-mqtt5-endpoint-publisher-subscriber.json" })
    void should_received_published_messages(HttpClient httpClient) {
        httpClient
            .rxWebSocket("/test")
            .flatMapPublisher(websocket ->
                // Write text frame
                websocket
                    .writeTextMessage("message1")
                    // Write binary frame
                    .andThen(websocket.writeBinaryMessage(io.vertx.rxjava3.core.buffer.Buffer.buffer("message2")))
                    // Write final text frame
                    .andThen(websocket.writeTextMessage("message3"))
                    .<io.vertx.rxjava3.core.buffer.Buffer>toFlowable()
                    .mergeWith(websocket.toFlowable())
            )
            .test()
            .awaitCount(3)
            .assertValueAt(
                0,
                frame -> {
                    assertThat(frame).hasToString("message1");
                    return true;
                }
            )
            .assertValueAt(
                1,
                frame -> {
                    assertThat(frame).hasToString("message2");
                    return true;
                }
            )
            .assertValueAt(
                2,
                frame -> {
                    assertThat(frame).hasToString("message3");
                    return true;
                }
            )
            .assertNoErrors();
    }
}
