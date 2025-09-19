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
package io.gravitee.apim.integration.tests.http.failover;

import static org.assertj.core.api.Assertions.assertThat;

import com.graviteesource.entrypoint.http.get.HttpGetEntrypointConnectorFactory;
import com.graviteesource.entrypoint.http.post.HttpPostEntrypointConnectorFactory;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.integration.tests.fake.MessageFlowReadyPolicy;
import io.gravitee.apim.integration.tests.messages.AbstractMqtt5EndpointIntegrationTest;
import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import io.reactivex.rxjava3.subjects.Subject;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@Nested
@GatewayTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FailoverV4Mqtt5EndpointIntegrationTest extends AbstractMqtt5EndpointIntegrationTest {

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("http-get", EntrypointBuilder.build("http-get", HttpGetEntrypointConnectorFactory.class));
        entrypoints.putIfAbsent("http-post", EntrypointBuilder.build("http-post", HttpPostEntrypointConnectorFactory.class));
    }

    @Test
    @DeployApi("/apis/v4/http/failover/mqtt-two-endpoints.json")
    void should_publish_on_first_retry(HttpClient client) {
        JsonObject requestBody = new JsonObject();
        requestBody.put("field", "value");

        final Subject<Void> readyObs = ReplaySubject.create();
        final TestSubscriber<Mqtt5Publish> testSubscriber = subscribeToMqtt5(TEST_TOPIC, readyObs).take(1).test();

        readyObs
            .ignoreElements()
            .andThen(postMessage(client, "/test", requestBody, Map.of("X-Test-Header", "header-value")))
            .test()
            .awaitDone(30, TimeUnit.SECONDS);

        testSubscriber
            .awaitDone(10, TimeUnit.SECONDS)
            .assertValue(message -> {
                assertThat(message.getTopic()).hasToString(TEST_TOPIC);
                assertThat(message.getResponseTopic()).isEmpty();
                assertThat(message.isRetain()).isFalse();
                assertThat(message.getPayloadAsBytes()).isEqualTo(requestBody.toBuffer().getBytes());

                assertThat(message.getUserProperties().asList().stream().map(Object::toString)).containsAnyOf(
                    "(content-length, " + requestBody.toString().length() + ")",
                    "(host, " + mqtt5.getHost() + ":" + mqtt5.getMqttPort() + ")",
                    "(X-Test-Header, header-value)"
                );
                return true;
            });

        // Verify there is no message since retain is false
        subscribeToMqtt5(TEST_TOPIC).take(1, TimeUnit.SECONDS).test().awaitDone(10, TimeUnit.SECONDS).assertNoValues();
    }

    private Completable postMessage(HttpClient client, String requestURI, JsonObject requestBody, Map<String, String> headers) {
        return client
            .rxRequest(HttpMethod.POST, requestURI)
            .flatMap(request -> {
                headers.forEach(request::putHeader);
                return request.rxSend(requestBody.toString());
            })
            .flatMapCompletable(response -> {
                assertThat(response.statusCode()).isEqualTo(202);
                return response.rxBody().ignoreElement();
            });
    }

    @Test
    @DeployApi("/apis/v4/http/failover/mqtt-two-endpoints.json")
    void should_subscribe_on_first_retry(HttpClient client) {
        final int messageCount = 2;
        final List<Completable> readyObs = new ArrayList<>();

        final Single<HttpClientResponse> get = client
            .rxRequest(HttpMethod.GET, "/test")
            .flatMap(request -> {
                request.putHeader(HttpHeaderNames.ACCEPT.toString(), MediaType.APPLICATION_JSON);
                return request.send();
            })
            .doOnSuccess(response -> readyObs.add(MessageFlowReadyPolicy.readyObs(extractTransactionId(response))));

        final TestSubscriber<JsonObject> obs = Flowable.fromSingle(
            get.doOnSuccess(response -> assertThat(response.statusCode()).isEqualTo(200))
        )
            .concatWith(publishMessagesWhenReady(readyObs, TEST_TOPIC, MqttQos.EXACTLY_ONCE))
            .flatMap(response -> response.rxBody().flatMapPublisher(buffer -> extractMessages(buffer, extractTransactionId(response))))
            .take(messageCount)
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValueCount(messageCount);

        verifyMessagesAreOrdered(messageCount, obs);
        verifyMessagesAreUniques(messageCount, obs);

        verifyMessagesAreBetweenRange(0, messageCount, obs);
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

    @NonNull
    private Flowable<JsonObject> extractMessages(Buffer body, String transactionId) {
        final JsonObject jsonResponse = new JsonObject(body.toString());
        final JsonArray items = jsonResponse.getJsonArray("items");
        final List<JsonObject> messages = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            final JsonObject message = items.getJsonObject(i);
            message.put("transactionId", transactionId);
            message.put("counter", Integer.parseInt(message.getString("content").replaceFirst(".*message-", "")));
            messages.add(message);
        }

        return Flowable.fromIterable(messages);
    }
}
