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

import static com.graviteesource.entrypoint.websocket.WebSocketCloseStatus.TRY_AGAIN_LATER;
import static org.assertj.core.api.Assertions.assertThat;

import com.graviteesource.entrypoint.websocket.WebSocketEntrypointConnectorFactory;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.integration.tests.fake.MessageFlowReadyPolicy;
import io.gravitee.apim.integration.tests.messages.AbstractMqtt5EndpointIntegrationTest;
import io.gravitee.common.utils.RxHelper;
import io.gravitee.common.utils.UUID;
import io.gravitee.gateway.reactive.api.qos.Qos;
import io.gravitee.gateway.reactive.core.connection.ConnectionDrainManager;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Predicate;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.vertx.core.http.UpgradeRejectedException;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.WebSocket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@GatewayTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class WebsocketEntrypointMqtt5EndpointIntegrationTest extends AbstractMqtt5EndpointIntegrationTest {

    protected static final int MESSAGE_COUNT = 10;

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("websocket", EntrypointBuilder.build("websocket", WebSocketEntrypointConnectorFactory.class));
    }

    @ParameterizedTest
    @MethodSource("qosParameters")
    @DeployApi({ "/apis/v4/messages/mqtt5/mqtt5-endpoint-qos-auto.json", "/apis/v4/messages/mqtt5/mqtt5-endpoint-qos-none.json" })
    void should_receive_messages_single(Qos qos, MqttQos publishQos, boolean expectExactRange, HttpClient httpClient) {
        final int messageCount = 10;
        final List<Completable> readyObs = new ArrayList<>();

        final Single<WebSocket> ws = createWSRequest("/test-qos-" + qos.getLabel(), UUID.random().toString(), httpClient, readyObs);

        final TestSubscriber<JsonObject> obs = Flowable.fromSingle(ws)
            .concatWith(publishMessagesWhenReady(readyObs, TEST_TOPIC + "-qos-" + qos.getLabel(), publishQos))
            .flatMap(this::extractMessages)
            .take(messageCount)
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValueCount(messageCount);

        verifyMessagesAreOrdered(messageCount, obs);
        verifyMessagesAreUniques(messageCount, obs);

        if (expectExactRange) {
            verifyMessagesAreBetweenRange(0, messageCount, obs);
        }
    }

    @Test
    @DeployApi({ "/apis/v4/messages/mqtt5/mqtt5-endpoint-qos-auto.json" })
    void should_close_with_try_again_later_code_when_connection_is_drained(HttpClient httpClient) {
        final ConnectionDrainManager connectionDrainManager = applicationContext.getBean(ConnectionDrainManager.class);
        final int messageCountBeforeDrain = 10;
        final List<Completable> readyObs = new ArrayList<>();

        final Single<WebSocket> ws = createWSRequest("/test-qos-auto", UUID.random().toString(), httpClient, readyObs);
        final AtomicInteger counter = new AtomicInteger(0);
        final AtomicReference<Short> websocketStatus = new AtomicReference<>();

        ws
            .flatMapCompletable(webSocket ->
                Completable.ambArray(
                    publishMessagesWhenReady(readyObs, TEST_TOPIC + "-qos-auto", MqttQos.AT_LEAST_ONCE),
                    extractMessages(webSocket)
                        .doOnNext(buffer -> {
                            if (counter.incrementAndGet() > messageCountBeforeDrain) {
                                connectionDrainManager.requestDrain();
                            }
                        })
                        .ignoreElements()
                        .doOnComplete(() -> websocketStatus.set(webSocket.closeStatusCode()))
                )
            )
            .test()
            // Expect the flow to finish because of the connection drain.
            .awaitDone(10, TimeUnit.SECONDS)
            .assertComplete();

        assertThat(websocketStatus.get()).isEqualTo((short) TRY_AGAIN_LATER.code());
    }

    @ParameterizedTest
    @MethodSource("qosParameters")
    @DeployApi({ "/apis/v4/messages/mqtt5/mqtt5-endpoint-qos-auto.json", "/apis/v4/messages/mqtt5/mqtt5-endpoint-qos-none.json" })
    void should_receive_messages_parallel(Qos qos, MqttQos publishQos, boolean expectExactRange, HttpClient httpClient) {
        final int messageCount = 20;
        final List<Completable> readyObs = new ArrayList<>();
        final String clientIdentifier = UUID.random().toString();

        // Note: use the same client identifier for both requests.
        final Single<WebSocket> ws1 = createWSRequest("/test-qos-" + qos.getLabel(), clientIdentifier, httpClient, readyObs);
        final Single<WebSocket> ws2 = createWSRequest("/test-qos-" + qos.getLabel(), clientIdentifier, httpClient, readyObs);

        final TestSubscriber<JsonObject> obs = ws1
            .mergeWith(ws2)
            .concatWith(publishMessagesWhenReady(readyObs, TEST_TOPIC + "-qos-" + qos.getLabel(), publishQos))
            .groupBy(this::extractTransactionId)
            .flatMap(groups -> groups.flatMap(this::extractMessages))
            .take(messageCount)
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValueCount(messageCount);

        verifyMessagesAreOrdered(messageCount, obs);
        verifyMessagesAreUniques(messageCount, obs);

        if (expectExactRange) {
            verifyMessagesAreBetweenRange(0, messageCount, obs);
        }
    }

    @ParameterizedTest
    @MethodSource("qosParameters")
    @DeployApi({ "/apis/v4/messages/mqtt5/mqtt5-endpoint-qos-auto.json", "/apis/v4/messages/mqtt5/mqtt5-endpoint-qos-none.json" })
    void should_receive_messages_sequential(Qos qos, MqttQos publishQos, boolean expectExactRange, HttpClient httpClient) {
        final int messageCount = 40;
        final List<Completable> readyObs = new ArrayList<>();
        final String clientIdentifier = UUID.random().toString();

        final Single<WebSocket> ws = createWSRequest("/test-qos-" + qos.getLabel(), clientIdentifier, httpClient, readyObs);

        final TestSubscriber<JsonObject> obs = Flowable.fromSingle(ws)
            .concatWith(publishMessagesWhenReady(readyObs, TEST_TOPIC + "-qos-" + qos.getLabel(), publishQos))
            .flatMap(webSocket ->
                extractMessages(webSocket)
                    .take(10)
                    .concatWith(
                        Single.defer(() ->
                            createWSRequest("/test-qos-" + qos.getLabel(), clientIdentifier, httpClient).delaySubscription(
                                500,
                                TimeUnit.MILLISECONDS
                            )
                        )
                            .flatMapPublisher(this::extractMessages)
                            .take(10)
                    )
                    .repeat(3)
            )
            .take(messageCount)
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValueCount(messageCount);

        verifyMessagesAreOrdered(messageCount, obs);
        verifyMessagesAreUniques(messageCount, obs);
        // Note: we can't guarantee there is no loss with sse because messages could be acknowledged and sent to the wire while client has closed the request.
    }

    @Test
    @DeployApi({ "/apis/v4/messages/mqtt5/mqtt5-endpoint-publish.json" })
    void should_publish_messages(HttpClient httpClient) {
        final String topic = TEST_TOPIC + "-publish";
        final TestSubscriber<Mqtt5Publish> testSubscriber = subscribeToMqtt5(topic).take(1).test();

        createWSRequest("/test-publish", UUID.random().toString(), httpClient)
            .flatMapCompletable(webSocket -> webSocket.writeFinalTextFrame("message"))
            .delaySubscription(100, TimeUnit.MILLISECONDS) // No way to know if mqtt5 subscription is ok, let's apply a small delay.
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertComplete()
            .assertNoErrors();

        testSubscriber
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValue(frame -> {
                assertThat(Buffer.buffer(frame.getPayloadAsBytes())).hasToString("message");
                return true;
            });
    }

    @EnumSource(value = Qos.class, names = { "AT_MOST_ONCE", "AT_LEAST_ONCE" })
    @ParameterizedTest(name = "should reject websocket upgrade with {0} qos")
    @DeployApi(
        { "/apis/v4/messages/mqtt5/mqtt5-endpoint-qos-at-least-once.json", "/apis/v4/messages/mqtt5/mqtt5-endpoint-qos-at-most-once.json" }
    )
    void should_reject_websocket_upgrade_with_unsupported_qos(Qos qos, HttpClient httpClient) {
        final TestObserver<WebSocket> obs = createWSRequest("/test-qos-" + qos.getLabel(), UUID.random().toString(), httpClient).test();

        obs.awaitDone(30, TimeUnit.SECONDS).assertError(UpgradeRejectedException.class);
    }

    @NonNull
    private Single<WebSocket> createWSRequest(String path, String clientIdentifier, HttpClient httpClient) {
        final String transactionId = UUID.random().toString();
        WebSocketConnectOptions options = new WebSocketConnectOptions();
        options.setURI(path);
        options.putHeader("X-Gravitee-Client-Identifier", clientIdentifier);
        options.putHeader("X-Gravitee-Transaction-Id", transactionId); // Force transaction id as it is not possible to retrieve it in the websocket response.

        return httpClient
            .rxWebSocket(options)
            .doOnSuccess(webSocket -> webSocket.headers().add("X-Gravitee-Transaction-Id", transactionId));
    }

    @NonNull
    private Single<WebSocket> createWSRequest(String path, String clientIdentifier, HttpClient httpClient, List<Completable> readyObs) {
        return createWSRequest(path, clientIdentifier, httpClient).doOnSuccess(webSocket ->
            readyObs.add(MessageFlowReadyPolicy.readyObs(extractTransactionId(webSocket)))
        );
    }

    @NonNull
    private Flowable<JsonObject> extractMessages(WebSocket webSocket) {
        final String transactionId = extractTransactionId(webSocket);
        // Map to a json structure to ease the assertions.
        return webSocket
            .toFlowable()
            .map(buffer -> {
                final String content = buffer.toString();
                return JsonObject.of("transactionId", transactionId)
                    .put("counter", Integer.parseInt(content.replaceFirst(".*message-", "")))
                    .put("content", content);
            });
    }

    private void verifyMessagesAreOrdered(int messageCount, TestSubscriber<JsonObject> obs) {
        final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();

        for (int i = 0; i < messageCount; i++) {
            obs.assertValueAt(i, jsonObject -> {
                final Integer messageCounter = jsonObject.getInteger("counter");
                final AtomicInteger requestCounter = counters.computeIfAbsent(jsonObject.getString("transactionId"), s ->
                    new AtomicInteger(messageCounter)
                );

                // A same request must receive a subset of all messages but always in order (ie: can't receive message-3 then message-1).
                assertThat(messageCounter).isGreaterThanOrEqualTo(requestCounter.get());
                requestCounter.set(messageCounter);
                assertThat(jsonObject.getString("content")).matches("message-" + messageCounter);

                return true;
            });
        }
    }

    private void verifyMessagesAreUniques(int messageCount, TestSubscriber<JsonObject> obs) {
        final HashSet<String> messages = new HashSet<>();

        for (int i = 0; i < messageCount; i++) {
            obs.assertValueAt(i, jsonObject -> {
                final String content = jsonObject.getString("content");
                assertThat(messages.contains(content)).isFalse();
                messages.add(content);

                return true;
            });
        }
    }

    private void verifyMessagesAreBetweenRange(int start, int end, TestSubscriber<JsonObject> obs) {
        final HashSet<Integer> messages = new HashSet<>();

        for (int i = 0; i < end - start; i++) {
            obs.assertValueAt(i, jsonObject -> {
                final Integer messageCounter = jsonObject.getInteger("counter");

                assertThat(messageCounter).isGreaterThanOrEqualTo(start);
                assertThat(messageCounter).isLessThan(end);
                messages.add(messageCounter);

                return true;
            });
        }

        assertThat(messages.size()).isEqualTo(end - start);
    }

    protected String extractTransactionId(WebSocket webSocket) {
        return webSocket.headers().get("X-Gravitee-Transaction-Id");
    }
}
