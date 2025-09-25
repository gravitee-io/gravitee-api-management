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
package io.gravitee.apim.integration.tests.messages.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import com.graviteesource.entrypoint.websocket.WebSocketEntrypointConnectorFactory;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.integration.tests.messages.AbstractKafkaEndpointIntegrationTest;
import io.gravitee.gateway.reactive.api.qos.Qos;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.WebSocket;
import io.vertx.rxjava3.kafka.client.consumer.KafkaConsumer;
import io.vertx.rxjava3.kafka.client.producer.KafkaProducer;
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
class WebsocketEntrypointKafkaEndpointIntegrationTest extends AbstractKafkaEndpointIntegrationTest {

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("websocket", EntrypointBuilder.build("websocket", WebSocketEntrypointConnectorFactory.class));
    }

    @EnumSource(value = Qos.class, names = { "NONE", "AUTO" })
    @ParameterizedTest(name = "should receive all messages with {0} qos")
    @DeployApi(
        {
            "/apis/v4/messages/websocket/websocket-entrypoint-kafka-endpoint-subscriber-none.json",
            "/apis/v4/messages/websocket/websocket-entrypoint-kafka-endpoint-subscriber.json",
        }
    )
    void should_receive_all_messages_with_qos(Qos qos, HttpClient httpClient, Vertx vertx) {
        // In order to simplify the test, Kafka endpoint's consumer is configured with "autoOffsetReset": "earliest"
        // It allows us to publish the messages in the topic before opening the api connection through entrypoint.
        KafkaProducer<String, byte[]> producer = getKafkaProducer(vertx);
        blockingPublishToKafka(producer, "message1");
        blockingPublishToKafka(producer, "message2");
        blockingPublishToKafka(producer, "message3");
        producer.close();

        final TestSubscriber<Buffer> obs = httpClient.rxWebSocket("/test-" + qos.getLabel()).flatMapPublisher(WebSocket::toFlowable).test();

        // We expect 3 binary frame, for 3 messages
        obs
            .awaitCount(3)
            .assertValueAt(0, frame -> {
                assertThat(frame).hasToString("message1");
                return true;
            })
            .assertValueAt(1, frame -> {
                assertThat(frame).hasToString("message2");
                return true;
            })
            .assertValueAt(2, frame -> {
                assertThat(frame).hasToString("message3");
                return true;
            })
            .assertNoErrors();
    }

    @Test
    @DeployApi({ "/apis/v4/messages/websocket/websocket-entrypoint-kafka-endpoint-publisher.json" })
    void should_publish_messages(HttpClient httpClient, Vertx vertx) {
        httpClient
            .rxWebSocket("/test")
            .flatMapCompletable(websocket ->
                // Write text frame
                websocket
                    .writeTextMessage("message1")
                    // Write binary frame
                    .andThen(websocket.writeBinaryMessage(Buffer.buffer("message2")))
                    // Write final text frame
                    .andThen(websocket.writeFinalTextFrame("message3"))
            )
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertComplete();

        // Configure a KafkaConsumer to read messages published on topic test-topic.
        KafkaConsumer<String, byte[]> kafkaConsumer = getKafkaConsumer(vertx);
        subscribeToKafka(kafkaConsumer)
            // We expect 3 message for this test
            .take(3)
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValueAt(0, message -> {
                assertThat(new String(message.value())).hasToString("message1");
                return true;
            })
            .assertValueAt(1, message -> {
                assertThat(new String(message.value())).hasToString("message2");
                return true;
            })
            .assertValueAt(2, message -> {
                assertThat(new String(message.value())).hasToString("message3");
                return true;
            })
            .assertComplete();

        kafkaConsumer.close().blockingAwait(30, TimeUnit.SECONDS);
    }

    @Test
    @DeployApi({ "/apis/v4/messages/websocket/websocket-entrypoint-kafka-endpoint-publisher-subscriber.json" })
    void should_received_published_messages(HttpClient httpClient, Vertx vertx) {
        httpClient
            .rxWebSocket("/test")
            .flatMapPublisher(websocket ->
                // Write text frame
                websocket
                    .writeTextMessage("message1")
                    // Write binary frame
                    .andThen(websocket.writeBinaryMessage(Buffer.buffer("message2")))
                    // Write final text frame
                    .andThen(websocket.writeTextMessage("message3"))
                    .<Buffer>toFlowable()
                    .mergeWith(websocket.toFlowable())
            )
            .test()
            .awaitCount(3)
            .assertValueAt(0, frame -> {
                assertThat(frame).hasToString("message1");
                return true;
            })
            .assertValueAt(1, frame -> {
                assertThat(frame).hasToString("message2");
                return true;
            })
            .assertValueAt(2, frame -> {
                assertThat(frame).hasToString("message3");
                return true;
            })
            .assertNoErrors();

        // Configure a KafkaConsumer to read messages published on topic test-topic.
        KafkaConsumer<String, byte[]> kafkaConsumer = getKafkaConsumer(vertx);
        subscribeToKafka(kafkaConsumer)
            // We expect 3 message for this test
            .take(3)
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValueAt(0, message -> {
                assertThat(new String(message.value())).hasToString("message1");
                return true;
            })
            .assertValueAt(1, message -> {
                assertThat(new String(message.value())).hasToString("message2");
                return true;
            })
            .assertValueAt(2, message -> {
                assertThat(new String(message.value())).hasToString("message3");
                return true;
            })
            .assertComplete();

        kafkaConsumer.close().blockingAwait(30, TimeUnit.SECONDS);
    }
}
