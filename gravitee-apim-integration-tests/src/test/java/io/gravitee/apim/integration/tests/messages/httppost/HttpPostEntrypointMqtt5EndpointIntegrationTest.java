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
package io.gravitee.apim.integration.tests.messages.httppost;

import static org.assertj.core.api.Assertions.assertThat;

import com.graviteesource.entrypoint.http.post.HttpPostEntrypointConnectorFactory;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.apim.integration.tests.fake.InterruptMessageRequestPhasePolicy;
import io.gravitee.apim.integration.tests.messages.AbstractMqtt5EndpointIntegrationTest;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.policy.assignattributes.AssignAttributesPolicy;
import io.gravitee.policy.assignattributes.configuration.AssignAttributesPolicyConfiguration;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import io.reactivex.rxjava3.subjects.Subject;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.http.HttpClient;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@GatewayTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class HttpPostEntrypointMqtt5EndpointIntegrationTest extends AbstractMqtt5EndpointIntegrationTest {

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("http-post", EntrypointBuilder.build("http-post", HttpPostEntrypointConnectorFactory.class));
    }

    @Override
    public void configurePolicies(Map<String, PolicyPlugin> policies) {
        super.configurePolicies(policies);

        policies.put(
            "interrupt-message-request-phase",
            PolicyBuilder.build("interrupt-message-request-phase", InterruptMessageRequestPhasePolicy.class)
        );
        policies.put(
            "assign-attributes",
            PolicyBuilder.build("assign-attributes", AssignAttributesPolicy.class, AssignAttributesPolicyConfiguration.class)
        );
    }

    @Test
    @DeployApi({ "/apis/v4/messages/http-post/http-post-entrypoint-mqtt5-endpoint.json" })
    void should_be_able_to_publish_to_mqtt5_endpoint_with_http_post_entrypoint(HttpClient client) {
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

    @Test
    @DeployApi({ "/apis/v4/messages/http-post/http-post-entrypoint-mqtt5-endpoint-retained.json" })
    void should_be_able_to_publish_retained_message_to_mqtt5_endpoint_with_http_post_entrypoint(HttpClient client) {
        JsonObject requestBody = new JsonObject();
        requestBody.put("field", "value-retained");

        final Subject<Void> readyObs = ReplaySubject.create();
        final TestSubscriber<Mqtt5Publish> testSubscriber = subscribeToMqtt5(TEST_TOPIC_RETAINED, readyObs).take(1).test();

        readyObs
            .ignoreElements()
            .andThen(
                postMessage(client, "/http-post-endpoint-mqtt5-endpoint-retained", requestBody, Map.of("X-Test-Header", "header-value"))
            )
            .test()
            .awaitDone(30, TimeUnit.SECONDS);

        // Check if message is present in Mqtt
        testSubscriber
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValueAt(0, message -> {
                assertThat(message.getTopic()).hasToString(TEST_TOPIC_RETAINED);
                assertThat(message.getResponseTopic()).isEmpty();
                assertThat(message.getPayloadAsBytes()).isEqualTo(requestBody.toBuffer().getBytes());

                assertThat(message.getUserProperties().asList().stream().map(Object::toString)).containsAnyOf(
                    "(content-length, " + requestBody.toString().length() + ")",
                    "(host, " + mqtt5.getHost() + ":" + mqtt5.getMqttPort() + ")",
                    "(X-Test-Header, header-value)"
                );
                return true;
            })
            .assertComplete();

        // Verify the message is still present because it is retained
        subscribeToMqtt5(TEST_TOPIC_RETAINED)
            .take(1)
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertValueAt(0, message -> {
                assertThat(message.getTopic()).hasToString(TEST_TOPIC_RETAINED);
                assertThat(message.getResponseTopic()).isEmpty();
                assertThat(message.getPayloadAsBytes()).isEqualTo(requestBody.toBuffer().getBytes());

                assertThat(message.getUserProperties().asList().stream().map(Object::toString)).containsAnyOf(
                    "(content-length, " + requestBody.toString().length() + ")",
                    "(host, " + mqtt5.getHost() + ":" + mqtt5.getMqttPort() + ")",
                    "(X-Test-Header, header-value)"
                );
                return true;
            })
            .assertComplete();
    }

    @Test
    @DeployApi({ "/apis/v4/messages/http-post/http-post-entrypoint-mqtt5-endpoint-attribute.json" })
    void should_be_able_to_publish_message_to_mqtt5_endpoint_with_http_post_entrypoint_topic_overridden_by_attribute(HttpClient client) {
        JsonObject requestBody = new JsonObject();
        requestBody.put("field", "value");

        JsonObject requestBodyAttribute = new JsonObject();
        requestBody.put("field", "value-attribute");

        final Subject<Void> readyObs1 = ReplaySubject.create();
        final Subject<Void> readyObs2 = ReplaySubject.create();
        final TestSubscriber<Mqtt5Publish> mqttTestSubscriberAttribute = subscribeToMqtt5(TEST_TOPIC_ATTRIBUTE, readyObs1).take(1).test();
        final TestSubscriber<Mqtt5Publish> mqttTestSubscriber = subscribeToMqtt5(TEST_TOPIC, readyObs2).take(1).test();

        Completable.mergeArray(readyObs1.ignoreElements(), readyObs2.ignoreElements())
            .andThen(
                Completable.mergeArray(
                    // Post a message with the header allowing to override topic
                    postMessage(client, "/topic-from-attribute", requestBodyAttribute, Map.of("X-New-Topic", TEST_TOPIC_ATTRIBUTE)),
                    // Post a message that will be published to topic configured for endpoint
                    postMessage(client, "/topic-from-attribute", requestBody, Map.of())
                )
            )
            .test()
            .awaitDone(30, TimeUnit.SECONDS);

        // Check if message is present in Mqtt test-topic-attribute
        mqttTestSubscriberAttribute
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValueAt(0, message -> {
                assertThat(message.getTopic()).hasToString(TEST_TOPIC_ATTRIBUTE);
                assertThat(message.getResponseTopic()).isEmpty();
                assertThat(message.getPayloadAsBytes()).isEqualTo(requestBodyAttribute.toBuffer().getBytes());

                assertThat(message.getUserProperties().asList().stream().map(Object::toString)).containsAnyOf(
                    "(content-length, " + requestBodyAttribute.toString().length() + ")",
                    "(host, " + mqtt5.getHost() + ":" + mqtt5.getMqttPort() + ")",
                    "(X-New-Topic, " + TEST_TOPIC_ATTRIBUTE + ")"
                );
                return true;
            })
            .assertComplete();

        mqttTestSubscriber
            .awaitDone(10, TimeUnit.SECONDS)
            .assertValueAt(0, message -> {
                assertThat(message.getTopic()).hasToString(TEST_TOPIC);
                assertThat(message.getResponseTopic()).isEmpty();
                assertThat(message.getPayloadAsBytes()).isEqualTo(requestBody.toBuffer().getBytes());

                assertThat(message.getUserProperties().asList().stream().map(Object::toString)).containsAnyOf(
                    "(content-length, " + requestBody.toString().length() + ")",
                    "(host, " + mqtt5.getHost() + ":" + mqtt5.getMqttPort() + ")"
                );
                return true;
            })
            .assertComplete();
    }

    @Test
    @DeployApi({ "/apis/v4/messages/http-post/http-post-entrypoint-mqtt5-endpoint-failure.json" })
    void should_return_an_error_when_message_failed_to_be_published(HttpClient client) {
        JsonObject requestBody = new JsonObject();
        requestBody.put("field", "value");

        final Subject<Void> readyObs = ReplaySubject.create();
        final TestSubscriber<Mqtt5Publish> testSubscriber = subscribeToMqtt5(TEST_TOPIC_FAILURE, readyObs).take(1).test();

        readyObs
            .ignoreElements()
            .andThen(
                client
                    .rxRequest(HttpMethod.POST, "/http-post-entrypoint-mqtt5-endpoint-failure")
                    .flatMap(request -> request.rxSend(requestBody.toString()))
                    .flatMap(response -> {
                        assertThat(response.statusCode()).isEqualTo(412);
                        return response.body();
                    })
            )
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertComplete()
            .assertValue(buffer -> {
                assertThat(buffer).hasToString("An error occurred");
                return true;
            });

        testSubscriber.awaitDone(10, TimeUnit.SECONDS).assertNoValues().assertNotComplete();
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
}
