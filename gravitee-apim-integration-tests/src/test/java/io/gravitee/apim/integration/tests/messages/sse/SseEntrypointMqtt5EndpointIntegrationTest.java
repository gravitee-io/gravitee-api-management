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
import com.hivemq.client.mqtt.datatypes.MqttQos;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.integration.tests.fake.MessageFlowReadyPolicy;
import io.gravitee.apim.integration.tests.messages.AbstractMqtt5EndpointIntegrationTest;
import io.gravitee.apim.integration.tests.messages.sse.utils.SseEvent;
import io.gravitee.apim.integration.tests.messages.sse.utils.SseEventHandler;
import io.gravitee.common.http.MediaType;
import io.gravitee.common.utils.UUID;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.reactive.api.qos.Qos;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientResponse;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@GatewayTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SseEntrypointMqtt5EndpointIntegrationTest extends AbstractMqtt5EndpointIntegrationTest {

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("sse", EntrypointBuilder.build("sse", SseEntrypointConnectorFactory.class));
    }

    @ParameterizedTest
    @MethodSource("qosParameters")
    @DeployApi({ "/apis/v4/messages/mqtt5/mqtt5-endpoint-qos-auto.json", "/apis/v4/messages/mqtt5/mqtt5-endpoint-qos-none.json" })
    void should_receive_messages_single(Qos qos, MqttQos publishQos, boolean expectExactRange, HttpClient httpClient) {
        final int messageCount = 10;
        final List<Completable> readyObs = new ArrayList<>();

        final Single<HttpClientResponse> sse = createSseRequest(
            "/test-qos-" + qos.getLabel(),
            UUID.random().toString(),
            httpClient,
            readyObs
        );

        final TestSubscriber<SseEvent> obs = Flowable.fromSingle(
            sse.doOnSuccess(response -> assertThat(response.statusCode()).isEqualTo(200))
        )
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

    @ParameterizedTest
    @MethodSource("qosParameters")
    @DeployApi({ "/apis/v4/messages/mqtt5/mqtt5-endpoint-qos-auto.json", "/apis/v4/messages/mqtt5/mqtt5-endpoint-qos-none.json" })
    void should_receive_messages_parallel(Qos qos, MqttQos publishQos, boolean expectExactRange, HttpClient httpClient) {
        final int messageCount = 20;
        final List<Completable> readyObs = new ArrayList<>();
        final String clientIdentifier = UUID.random().toString();

        // Note: use the same client identifier for both requests.
        final Single<HttpClientResponse> sse1 = createSseRequest("/test-qos-" + qos.getLabel(), clientIdentifier, httpClient, readyObs);
        final Single<HttpClientResponse> sse2 = createSseRequest("/test-qos-" + qos.getLabel(), clientIdentifier, httpClient, readyObs);

        final TestSubscriber<SseEvent> obs = sse1
            .mergeWith(sse2)
            .doOnNext(response -> assertThat(response.statusCode()).isEqualTo(200))
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

        final Single<HttpClientResponse> sse = createSseRequest("/test-qos-" + qos.getLabel(), clientIdentifier, httpClient, readyObs);

        final TestSubscriber<SseEvent> obs = Flowable.fromSingle(
            sse.doOnSuccess(response -> assertThat(response.statusCode()).isEqualTo(200))
        )
            .concatWith(publishMessagesWhenReady(readyObs, TEST_TOPIC + "-qos-" + qos.getLabel(), publishQos))
            .flatMap(response ->
                extractMessages(response)
                    .concatWith(
                        Single.defer(() ->
                            createSseRequest("/test-qos-" + qos.getLabel(), clientIdentifier, httpClient).delaySubscription(
                                500,
                                TimeUnit.MILLISECONDS
                            )
                        )
                            .doOnSuccess(nextResponse -> assertThat(nextResponse.statusCode()).isEqualTo(200))
                            .flatMapPublisher(this::extractMessages)
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

    @EnumSource(value = Qos.class, names = { "AT_MOST_ONCE", "AT_LEAST_ONCE" })
    @ParameterizedTest(name = "should receive 400 bad request with {0} qos")
    @DeployApi(
        { "/apis/v4/messages/mqtt5/mqtt5-endpoint-qos-at-least-once.json", "/apis/v4/messages/mqtt5/mqtt5-endpoint-qos-at-most-once.json" }
    )
    void should_receive_400_bad_request_with_qos(Qos qos, HttpClient httpClient) {
        final TestObserver<Buffer> obs = createSseRequest("/test-qos-" + qos.getLabel(), UUID.random().toString(), httpClient)
            .doOnSuccess(response -> assertThat(response.statusCode()).isEqualTo(400))
            .flatMap(HttpClientResponse::body)
            .test();

        obs
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValue(body -> {
                assertThat(body.toString()).isEqualTo(
                    "Incompatible Qos capabilities between entrypoint requirements and endpoint supports"
                );

                return true;
            });
    }

    @Test
    @DeployApi({ "/apis/v4/messages/mqtt5/mqtt5-endpoint-failure.json" })
    void should_receive_error_messages_when_error_occurred(HttpClient httpClient) {
        final TestObserver<Buffer> obs = createSseRequest("/test-failure", UUID.random().toString(), httpClient)
            .doOnSuccess(response -> assertThat(response.statusCode()).isEqualTo(500))
            .flatMap(HttpClientResponse::body)
            .test();

        obs
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValue(body -> {
                assertThat(body.toString()).isEqualTo("Endpoint connection failed");

                return true;
            });
    }

    @NonNull
    private Single<HttpClientResponse> createSseRequest(String path, String clientIdentifier, HttpClient httpClient) {
        return httpClient
            .rxRequest(HttpMethod.GET, path)
            .flatMap(request -> {
                request.putHeader(HttpHeaderNames.ACCEPT.toString(), MediaType.TEXT_EVENT_STREAM);
                request.putHeader("X-Gravitee-Client-Identifier", clientIdentifier);
                return request.rxSend();
            });
    }

    @NonNull
    private Single<HttpClientResponse> createSseRequest(
        String path,
        String clientIdentifier,
        HttpClient httpClient,
        List<Completable> readyObs
    ) {
        return createSseRequest(path, clientIdentifier, httpClient).doOnSuccess(response ->
            readyObs.add(MessageFlowReadyPolicy.readyObs(extractTransactionId(response)))
        );
    }

    @NonNull
    private Flowable<SseEvent> extractMessages(HttpClientResponse response) {
        final SseEventHandler sseEventHandler = new SseEventHandler(extractTransactionId(response));
        return response.toFlowable().concatMapMaybe(sseEventHandler::handleMessage);
    }

    private void verifyMessagesAreOrdered(int messageCount, TestSubscriber<SseEvent> obs) {
        final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();

        for (int i = 0; i < messageCount; i++) {
            obs.assertValueAt(i, sseEvent -> {
                final Integer messageCounter = sseEvent.getCounter();
                final AtomicInteger requestCounter = counters.computeIfAbsent(sseEvent.getRequestId(), s ->
                    new AtomicInteger(messageCounter)
                );

                // A same request must receive a subset of all messages but always in order (ie: can't receive message-3 then message-1).
                assertThat(messageCounter).isGreaterThanOrEqualTo(requestCounter.get());
                requestCounter.set(messageCounter);
                assertThat(sseEvent.getData()).matches("message-" + messageCounter);

                return true;
            });
        }
    }

    private void verifyMessagesAreUniques(int messageCount, TestSubscriber<SseEvent> obs) {
        final HashSet<String> messages = new HashSet<>();

        for (int i = 0; i < messageCount; i++) {
            obs.assertValueAt(i, sseEvent -> {
                final String content = sseEvent.getData();
                assertThat(messages.contains(content)).isFalse();
                messages.add(content);

                return true;
            });
        }
    }

    private void verifyMessagesAreBetweenRange(int start, int end, TestSubscriber<SseEvent> obs) {
        final HashSet<Integer> messages = new HashSet<>();

        for (int i = 0; i < end - start; i++) {
            obs.assertValueAt(i, sseEvent -> {
                final Integer messageCounter = sseEvent.getCounter();

                assertThat(messageCounter).isGreaterThanOrEqualTo(start);
                assertThat(messageCounter).isLessThan(end);
                messages.add(messageCounter);

                return true;
            });
        }

        assertThat(messages.size()).isEqualTo(end - start);
    }
}
