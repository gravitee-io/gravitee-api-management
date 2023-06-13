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
package io.gravitee.apim.integration.tests.messages.httppost;

import static org.assertj.core.api.Assertions.assertThat;

import com.graviteesource.entrypoint.http.post.HttpPostEntrypointConnectorFactory;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import hu.akarnokd.rxjava3.bridge.RxJavaBridge;
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
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.buffer.Buffer;
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
    @DeployApi({ "/apis/v4/messages/http-post-entrypoint-mqtt5-endpoint.json" })
    void should_be_able_to_publish_to_mqtt5_endpoint_with_httppost_entrypoint(HttpClient client) {
        JsonObject requestBody = new JsonObject();
        requestBody.put("field", "value");

        subscribeToMqtt5Flowable(mqtt5RxClient, TEST_TOPIC)
            .zipWith(postMessage(client, "/test", requestBody, Map.of("X-Test-Header", "header-value")).isEmpty().toFlowable(), (c, o) -> c)
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValue(message -> {
                assertThat(message.getTopic()).hasToString(TEST_TOPIC);
                assertThat(message.getResponseTopic()).isEmpty();
                assertThat(message.isRetain()).isFalse();
                assertThat(message.getPayloadAsBytes()).isEqualTo(requestBody.toBuffer().getBytes());

                assertThat(message.getUserProperties().asList().stream().map(Object::toString))
                    .containsAnyOf(
                        "(content-length, " + requestBody.toString().length() + ")",
                        "(host, " + mqtt5.getHost() + ":" + mqtt5.getMqttPort() + ")",
                        "(X-Test-Header, header-value)"
                    );
                return true;
            });

        // Verify there is no message since retain is false
        RxJavaBridge
            .toV3Flowable(mqtt5RxClient.subscribePublishesWith().topicFilter(TEST_TOPIC).qos(Mqtt5Publish.DEFAULT_QOS).applySubscribe())
            .test()
            .awaitDone(1, TimeUnit.SECONDS)
            .assertNoValues()
            .assertNotComplete();
    }

    @Test
    @DeployApi({ "/apis/v4/messages/http-post-entrypoint-mqtt5-endpoint-retained.json" })
    void should_be_able_to_publish_retained_message_to_mqtt5_endpoint_with_httppost_entrypoint(HttpClient client) {
        JsonObject requestBody = new JsonObject();
        requestBody.put("field", "value-retained");

        final TestSubscriber<Mqtt5Publish> mqttTestSubscriber = subscribeToMqtt5(mqtt5RxClient, TEST_TOPIC_RETAINED);

        // Post a message
        blockingPostMessage(client, "/http-post-endpoint-mqtt5-endpoint-retained", requestBody, Map.of("X-Test-Header", "header-value"));

        // Check if message is present in Mqtt
        mqttTestSubscriber
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValueAt(
                0,
                message -> {
                    assertThat(message.getTopic()).hasToString(TEST_TOPIC_RETAINED);
                    assertThat(message.getResponseTopic()).isEmpty();
                    assertThat(message.getPayloadAsBytes()).isEqualTo(requestBody.toBuffer().getBytes());

                    assertThat(message.getUserProperties().asList().stream().map(Object::toString))
                        .containsAnyOf(
                            "(content-length, " + requestBody.toString().length() + ")",
                            "(host, " + mqtt5.getHost() + ":" + mqtt5.getMqttPort() + ")",
                            "(X-Test-Header, header-value)"
                        );
                    return true;
                }
            )
            .assertComplete();

        // Verify the message is still present because it is retained
        RxJavaBridge
            .toV3Flowable(
                mqtt5RxClient.subscribePublishesWith().topicFilter(TEST_TOPIC_RETAINED).qos(Mqtt5Publish.DEFAULT_QOS).applySubscribe()
            )
            .take(1)
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertValueAt(
                0,
                message -> {
                    assertThat(message.getTopic()).hasToString(TEST_TOPIC_RETAINED);
                    assertThat(message.getResponseTopic()).isEmpty();
                    assertThat(message.getPayloadAsBytes()).isEqualTo(requestBody.toBuffer().getBytes());

                    assertThat(message.getUserProperties().asList().stream().map(Object::toString))
                        .containsAnyOf(
                            "(content-length, " + requestBody.toString().length() + ")",
                            "(host, " + mqtt5.getHost() + ":" + mqtt5.getMqttPort() + ")",
                            "(X-Test-Header, header-value)"
                        );
                    return true;
                }
            )
            .assertComplete();
    }

    @Test
    @DeployApi({ "/apis/v4/messages/http-post-entrypoint-mqtt5-endpoint-attribute.json" })
    void should_be_able_to_publish_message_to_mqtt5_endpoint_with_httppost_entrypoint_topic_overridden_by_attribute(HttpClient client) {
        JsonObject requestBody = new JsonObject();
        requestBody.put("field", "value");

        JsonObject requestBodyAttribute = new JsonObject();
        requestBody.put("field", "value-attribute");

        final TestSubscriber<Mqtt5Publish> mqttTestSubscriberAttribute = subscribeToMqtt5(mqtt5RxClient, TEST_TOPIC_ATTRIBUTE);
        final TestSubscriber<Mqtt5Publish> mqttTestSubscriber = subscribeToMqtt5(mqtt5RxClient, TEST_TOPIC);

        // Post a message with the header allowing to override topic
        blockingPostMessage(client, "/topic-from-attribute", requestBodyAttribute, Map.of("X-New-Topic", TEST_TOPIC_ATTRIBUTE));

        // Post a message that will be published to topic configured for endpoint
        blockingPostMessage(client, "/topic-from-attribute", requestBody, Map.of());

        // Check if message is present in Mqtt test-topic-attribute
        mqttTestSubscriberAttribute
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValueAt(
                0,
                message -> {
                    assertThat(message.getTopic()).hasToString(TEST_TOPIC_ATTRIBUTE);
                    assertThat(message.getResponseTopic()).isEmpty();
                    assertThat(message.getPayloadAsBytes()).isEqualTo(requestBodyAttribute.toBuffer().getBytes());

                    assertThat(message.getUserProperties().asList().stream().map(Object::toString))
                        .containsAnyOf(
                            "(content-length, " + requestBodyAttribute.toString().length() + ")",
                            "(host, " + mqtt5.getHost() + ":" + mqtt5.getMqttPort() + ")",
                            "(X-New-Topic, " + TEST_TOPIC_ATTRIBUTE + ")"
                        );
                    return true;
                }
            )
            .assertComplete();

        mqttTestSubscriber
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValueAt(
                0,
                message -> {
                    assertThat(message.getTopic()).hasToString(TEST_TOPIC);
                    assertThat(message.getResponseTopic()).isEmpty();
                    assertThat(message.getPayloadAsBytes()).isEqualTo(requestBody.toBuffer().getBytes());

                    assertThat(message.getUserProperties().asList().stream().map(Object::toString))
                        .containsAnyOf(
                            "(content-length, " + requestBody.toString().length() + ")",
                            "(host, " + mqtt5.getHost() + ":" + mqtt5.getMqttPort() + ")"
                        );
                    return true;
                }
            )
            .assertComplete();
    }

    @Test
    @DeployApi({ "/apis/v4/messages/http-post-entrypoint-mqtt5-endpoint-failure.json" })
    void should_return_an_error_when_message_failed_to_be_published(HttpClient client) {
        JsonObject requestBody = new JsonObject();
        requestBody.put("field", "value");

        final TestSubscriber<Mqtt5Publish> mqttTestSubscriber = subscribeToMqtt5(mqtt5RxClient, TEST_TOPIC_FAILURE);

        client
            .rxRequest(HttpMethod.POST, "/http-post-entrypoint-mqtt5-endpoint-failure")
            .flatMap(request -> request.rxSend(requestBody.toString()))
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(412);
                return response.body();
            })
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertComplete()
            .assertValue(buffer -> {
                assertThat(buffer).hasToString("An error occurred");
                return true;
            });

        mqttTestSubscriber.awaitDone(2, TimeUnit.SECONDS).assertNoValues().assertNotComplete();
    }

    private void blockingPostMessage(HttpClient client, String requestURI, JsonObject requestBody, Map<String, String> headers) {
        postMessage(client, requestURI, requestBody, headers)
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertNoValues()
            .assertComplete()
            .assertNoErrors();
    }

    private Flowable<Buffer> postMessage(HttpClient client, String requestURI, JsonObject requestBody, Map<String, String> headers) {
        return client
            .rxRequest(HttpMethod.POST, requestURI)
            .flatMap(request -> {
                headers.forEach(request::putHeader);
                return request.rxSend(requestBody.toString());
            })
            .flatMapPublisher(response -> {
                assertThat(response.statusCode()).isEqualTo(202);
                return response.toFlowable();
            });
    }
}
