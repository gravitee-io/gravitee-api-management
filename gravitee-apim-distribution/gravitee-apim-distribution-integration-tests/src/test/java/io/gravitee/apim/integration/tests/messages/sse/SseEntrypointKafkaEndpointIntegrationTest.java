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
import static org.awaitility.Awaitility.await;

import com.graviteesource.entrypoint.sse.SseEntrypointConnectorFactory;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.integration.tests.fake.MessageFlowReadyPolicy;
import io.gravitee.apim.integration.tests.messages.AbstractKafkaEndpointIntegrationTest;
import io.gravitee.apim.integration.tests.messages.sse.utils.SseEvent;
import io.gravitee.apim.integration.tests.messages.sse.utils.SseEventHandler;
import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.reactive.api.qos.Qos;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientResponse;
import io.vertx.rxjava3.kafka.client.producer.KafkaProducer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@GatewayTest
@DeployApi({ "/apis/v4/messages/sse/sse-entrypoint-kafka-endpoint.json" })
class SseEntrypointKafkaEndpointIntegrationTest extends AbstractKafkaEndpointIntegrationTest {

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("sse", EntrypointBuilder.build("sse", SseEntrypointConnectorFactory.class));
    }

    @Test
    @DisplayName("Should deploy a V4 API with an SSE entrypoint and Kafka endpoint")
    void shouldBeAbleToSubscribeToKafkaEndpointWithSSEEntrypoint(HttpClient httpClient, Vertx vertx) {
        // In order to simplify the test, Kafka endpoint's consumer is configured with "autoOffsetReset": "earliest"
        // It allows us to publish the messages in the topic before opening the api connection through SSE entrypoint.
        KafkaProducer<String, byte[]> producer = getKafkaProducer(vertx);
        blockingPublishToKafka(producer, "message1");
        blockingPublishToKafka(producer, "message2");
        blockingPublishToKafka(producer, "message3");
        producer.close();

        final TestSubscriber<Buffer> obs = httpClient
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

        // We expect 4 chunks, 1 retry message and 3 messages
        await()
            .atMost(30, TimeUnit.SECONDS)
            .until(() -> obs.values().size() >= 4);
        obs
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

    @ParameterizedTest
    @MethodSource("allQosParameters")
    @DeployApi(
        {
            "/apis/v4/messages/kafka/kafka-endpoint-qos-none.json",
            "/apis/v4/messages/kafka/kafka-endpoint-qos-auto.json",
            "/apis/v4/messages/kafka/kafka-endpoint-qos-at-most-once.json",
            "/apis/v4/messages/kafka/kafka-endpoint-qos-at-least-once.json",
        }
    )
    @DisplayName("Should receive the messages in order, each once, with an id only when the qos recovers")
    void shouldReceiveMessagesWithQos(Qos qos, boolean expectExactRange, HttpClient httpClient) {
        final int messageCount = 10;
        final String topic = TEST_TOPIC + "-qos-" + qos.getLabel();
        final List<Completable> readyObs = new ArrayList<>();

        final List<SseEvent> events = Flowable.fromSingle(
            createSseRequest("/test-qos-" + qos.getLabel(), UUID.randomUUID().toString(), null, httpClient).doOnSuccess(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                readyObs.add(MessageFlowReadyPolicy.readyObs(extractTransactionId(response)));
            })
        )
            .concatWith(publishMessagesWhenReady(readyObs, topic))
            .flatMap(this::extractMessages)
            .take(messageCount)
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValueCount(messageCount)
            .values();

        final List<Integer> counters = events.stream().map(SseEvent::getCounter).toList();
        assertThat(counters).doesNotHaveDuplicates().isSorted();
        if (expectExactRange) {
            assertThat(counters).containsExactlyElementsOf(IntStream.range(0, messageCount).boxed().toList());
        }
        if (qos == Qos.AT_MOST_ONCE || qos == Qos.AT_LEAST_ONCE) {
            // The entrypoint requires recovery for these qos, so each event carries the partition and offset of its record.
            assertThat(events).allSatisfy(event -> assertThat(decoded(event.getId())).isEqualTo(topic + "@0#" + event.getCounter()));
        } else {
            assertThat(events).allSatisfy(event -> assertThat(event.getId()).isNull());
        }
    }

    @ParameterizedTest
    @EnumSource(value = Qos.class, names = { "AT_MOST_ONCE", "AT_LEAST_ONCE" })
    @DeployApi(
        { "/apis/v4/messages/kafka/kafka-endpoint-qos-at-most-once.json", "/apis/v4/messages/kafka/kafka-endpoint-qos-at-least-once.json" }
    )
    @DisplayName("Should resume after the Last-Event-ID without losing or repeating a message")
    void shouldResumeAfterLastEventId(Qos qos, HttpClient httpClient, Vertx vertx) {
        final String topic = TEST_TOPIC + "-qos-" + qos.getLabel();
        final String path = "/test-qos-" + qos.getLabel();
        deleteTopic(topic);
        KafkaProducer<String, byte[]> producer = getKafkaProducer(vertx);
        for (int i = 0; i < 10; i++) {
            publishToKafka(producer, topic, "message-" + i).test().awaitDone(10, TimeUnit.SECONDS).assertComplete();
        }
        producer.close().blockingAwait(10, TimeUnit.SECONDS);

        try {
            final List<SseEvent> first = receive(path, UUID.randomUUID().toString(), null, 5, httpClient);
            assertThat(first).extracting(SseEvent::getCounter).containsExactly(0, 1, 2, 3, 4);

            // Another client, whose consumer group has committed nothing, resumes from the id alone.
            final List<SseEvent> resumed = receive(path, UUID.randomUUID().toString(), first.get(4).getId(), 5, httpClient);
            assertThat(resumed).extracting(SseEvent::getCounter).containsExactly(5, 6, 7, 8, 9);
        } finally {
            deleteTopic(topic);
        }
    }

    @Test
    @DeployApi({ "/apis/v4/messages/kafka/kafka-endpoint-topic-not-found.json" })
    @DisplayName("Should answer 404 when the topic does not exist")
    void shouldAnswerNotFoundWhenTopicDoesNotExist(HttpClient httpClient) {
        createSseRequest("/test-topic-not-found", UUID.randomUUID().toString(), null, httpClient)
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(404);
                return response.body();
            })
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValue(body -> {
                assertThat(body.toString()).isEqualTo("Topics not found");
                return true;
            });
    }

    @Test
    @DeployApi({ "/apis/v4/messages/kafka/kafka-endpoint-unreachable.json" })
    @DisplayName("Should end the stream with an error event when the broker cannot be reached")
    void shouldEndStreamWhenBrokerIsUnreachable(HttpClient httpClient) {
        final TestSubscriber<Buffer> obs = createSseRequest("/test-unreachable", UUID.randomUUID().toString(), null, httpClient)
            .flatMapPublisher(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return response.toFlowable();
            })
            .test()
            .awaitDone(60, TimeUnit.SECONDS)
            .assertComplete();

        assertThat(concat(obs.values())).contains("event: error\ndata: Endpoint connection closed\n\n");
    }

    @Test
    @DeployApi({ "/apis/v4/messages/kafka/kafka-endpoint-qos-auto.json" })
    @DisplayName("Should end the stream and leave the consumer group when the api is undeployed")
    void shouldEndStreamWhenApiIsUndeployed(HttpClient httpClient, Vertx vertx) throws Exception {
        final String topic = TEST_TOPIC + "-qos-auto";
        final String clientIdentifier = UUID.randomUUID().toString();
        deleteTopic(topic);
        KafkaProducer<String, byte[]> producer = getKafkaProducer(vertx);
        publishToKafka(producer, topic, "message-0").test().awaitDone(10, TimeUnit.SECONDS).assertComplete();
        producer.close().blockingAwait(10, TimeUnit.SECONDS);

        try {
            final TestSubscriber<Buffer> obs = createSseRequest("/test-qos-auto", clientIdentifier, null, httpClient)
                .flatMapPublisher(response -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    return response.toFlowable();
                })
                .test();
            await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> concat(obs.values()).contains("message-0"));
            // The gateway suffixes the client identifier, and the endpoint names the consumer group after it.
            assertThat(consumerGroupMembers(clientIdentifier)).isOne();

            undeploy("kafka-endpoint-qos-auto");

            obs.awaitDone(30, TimeUnit.SECONDS).assertComplete();
            assertThat(concat(obs.values())).contains("event: goaway");
            await()
                .atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(consumerGroupMembers(clientIdentifier)).isZero());
        } finally {
            deleteTopic(topic);
        }
    }

    private Single<HttpClientResponse> createSseRequest(String path, String clientIdentifier, String lastEventId, HttpClient httpClient) {
        return httpClient
            .rxRequest(HttpMethod.GET, path)
            .flatMap(request -> {
                request.putHeader(HttpHeaderNames.ACCEPT.toString(), MediaType.TEXT_EVENT_STREAM);
                request.putHeader("X-Gravitee-Client-Identifier", clientIdentifier);
                if (lastEventId != null) {
                    request.putHeader("Last-Event-ID", lastEventId);
                }
                return request.rxSend();
            });
    }

    private List<SseEvent> receive(String path, String clientIdentifier, String lastEventId, int count, HttpClient httpClient) {
        return createSseRequest(path, clientIdentifier, lastEventId, httpClient)
            .doOnSuccess(response -> assertThat(response.statusCode()).isEqualTo(200))
            .flatMapPublisher(this::extractMessages)
            .take(count)
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValueCount(count)
            .values();
    }

    private Flowable<SseEvent> extractMessages(HttpClientResponse response) {
        final SseEventHandler sseEventHandler = new SseEventHandler(extractTransactionId(response));
        return response.toFlowable().concatMapMaybe(sseEventHandler::handleMessage);
    }

    private static String extractTransactionId(HttpClientResponse response) {
        return response.getHeader("X-Gravitee-Transaction-Id");
    }

    private static String decoded(String id) {
        return new String(Base64.getDecoder().decode(id), StandardCharsets.UTF_8);
    }

    private static String concat(List<Buffer> chunks) {
        return chunks.stream().map(Buffer::toString).reduce("", String::concat);
    }
}
