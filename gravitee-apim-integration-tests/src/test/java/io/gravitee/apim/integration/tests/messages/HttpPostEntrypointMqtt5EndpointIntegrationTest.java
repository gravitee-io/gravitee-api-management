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
package io.gravitee.apim.integration.tests.messages;

import static org.assertj.core.api.Assertions.assertThat;

import com.graviteesource.endpoint.mqtt5.Mqtt5EndpointConnectorFactory;
import com.graviteesource.entrypoint.http.post.HttpPostEntrypointConnectorFactory;
import com.graviteesource.reactor.message.MessageApiReactorFactory;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.Mqtt5RxClient;
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAckReasonCode;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import hu.akarnokd.rxjava3.bridge.RxJavaBridge;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.apim.gateway.tests.sdk.reactor.ReactorBuilder;
import io.gravitee.apim.integration.tests.fake.InterruptMessageRequestPhasePolicy;
import io.gravitee.apim.plugin.reactor.ReactorPlugin;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactory;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.policy.assignattributes.AssignAttributesPolicy;
import io.gravitee.policy.assignattributes.configuration.AssignAttributesPolicyConfiguration;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rxjava3.core.http.HttpClient;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.testcontainers.hivemq.HiveMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Testcontainers
@GatewayTest
@DeployApi(
    {
        "/apis/v4/messages/http-post-entrypoint-mqtt5-endpoint.json",
        "/apis/v4/messages/http-post-entrypoint-mqtt5-endpoint-retained.json",
        "/apis/v4/messages/http-post-entrypoint-mqtt5-endpoint-failure.json",
        "/apis/v4/messages/http-post-entrypoint-mqtt5-endpoint-attribute.json",
    }
)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class HttpPostEntrypointMqtt5EndpointIntegrationTest extends AbstractGatewayTest {

    @Container
    private static final HiveMQContainer mqtt5 = new HiveMQContainer(DockerImageName.parse("hivemq/hivemq-ce").withTag("2023.3"))
        .withTmpFs(null);

    public static final String TEST_TOPIC = "test-topic";
    public static final String TEST_TOPIC_RETAINED = "test-topic-retained";
    public static final String TEST_TOPIC_FAILURE = "test-topic-failure";
    public static final String TEST_TOPIC_ATTRIBUTE = "test-topic-attribute";

    @Override
    public void configureReactors(Set<ReactorPlugin<? extends ReactorFactory<?>>> reactors) {
        reactors.add(ReactorBuilder.build(MessageApiReactorFactory.class));
    }

    @Override
    public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
        endpoints.putIfAbsent("mqtt5", EndpointBuilder.build("mqtt5", Mqtt5EndpointConnectorFactory.class));
    }

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

    @Override
    public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
        if (definitionClass.isAssignableFrom(Api.class)) {
            Api apiDefinition = (Api) api.getDefinition();
            apiDefinition
                .getEndpointGroups()
                .stream()
                .flatMap(eg -> eg.getEndpoints().stream())
                .filter(endpoint -> endpoint.getType().equals("mqtt5"))
                .forEach(endpoint ->
                    endpoint.setConfiguration(endpoint.getConfiguration().replace("mqtt5-port", Integer.toString(mqtt5.getMqttPort())))
                );
        }
    }

    @Test
    void should_be_able_to_publish_to_mqtt5_endpoint_with_httppost_entrypoint(HttpClient client, VertxTestContext testContext) {
        JsonObject requestBody = new JsonObject();
        requestBody.put("field", "value");

        final Mqtt5RxClient mqtt5RxClient = prepareMqtt5Client();

        connectToMqtt5(testContext, mqtt5RxClient);

        final TestSubscriber<Mqtt5Publish> mqttTestSubscriber = subscribeToMqtt5(mqtt5RxClient, TEST_TOPIC);

        // Post a message
        postMessage(client, "/test", requestBody, Map.of("X-Test-Header", "header-value"));

        // Check if message is present in Mqtt
        mqttTestSubscriber
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValueAt(
                0,
                message -> {
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
                }
            )
            .assertComplete();

        // Verify there is no message since retain is false
        RxJavaBridge
            .toV3Flowable(mqtt5RxClient.subscribePublishesWith().topicFilter(TEST_TOPIC).qos(Mqtt5Publish.DEFAULT_QOS).applySubscribe())
            .test()
            .awaitDone(1, TimeUnit.SECONDS)
            .assertNoValues()
            .assertNotComplete();

        disconnectFromMqtt5(mqtt5RxClient);
    }

    @Test
    void should_be_able_to_publish_retained_message_to_mqtt5_endpoint_with_httppost_entrypoint(
        HttpClient client,
        VertxTestContext testContext
    ) {
        JsonObject requestBody = new JsonObject();
        requestBody.put("field", "value-retained");

        final Mqtt5RxClient mqtt5RxClient = prepareMqtt5Client();

        connectToMqtt5(testContext, mqtt5RxClient);

        final TestSubscriber<Mqtt5Publish> mqttTestSubscriber = subscribeToMqtt5(mqtt5RxClient, TEST_TOPIC_RETAINED);

        // Post a message
        postMessage(client, "/http-post-endpoint-mqtt5-endpoint-retained", requestBody, Map.of("X-Test-Header", "header-value"));

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

        disconnectFromMqtt5(mqtt5RxClient);
    }

    @Test
    void should_be_able_to_publish_message_to_mqtt5_endpoint_with_httppost_entrypoint_topic_overridden_by_attribute(
        HttpClient client,
        VertxTestContext testContext
    ) {
        JsonObject requestBody = new JsonObject();
        requestBody.put("field", "value");

        JsonObject requestBodyAttribute = new JsonObject();
        requestBody.put("field", "value-attribute");

        final Mqtt5RxClient mqtt5RxClient = prepareMqtt5Client();

        connectToMqtt5(testContext, mqtt5RxClient);

        final TestSubscriber<Mqtt5Publish> mqttTestSubscriberAttribute = subscribeToMqtt5(mqtt5RxClient, TEST_TOPIC_ATTRIBUTE);
        final TestSubscriber<Mqtt5Publish> mqttTestSubscriber = subscribeToMqtt5(mqtt5RxClient, TEST_TOPIC);

        // Post a message with the header allowing to override topic
        postMessage(client, "/topic-from-attribute", requestBodyAttribute, Map.of("X-New-Topic", TEST_TOPIC_ATTRIBUTE));

        // Post a message that will be published to topic configured for endpoint
        postMessage(client, "/topic-from-attribute", requestBody, Map.of());

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

        disconnectFromMqtt5(mqtt5RxClient);
    }

    @Test
    void should_return_an_error_when_message_failed_to_be_published(HttpClient client, VertxTestContext testContext) {
        JsonObject requestBody = new JsonObject();
        requestBody.put("field", "value");

        final Mqtt5RxClient mqtt5RxClient = prepareMqtt5Client();

        connectToMqtt5(testContext, mqtt5RxClient);

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

        disconnectFromMqtt5(mqtt5RxClient);
    }

    private static void postMessage(HttpClient client, String requestURI, JsonObject requestBody, Map<String, String> headers) {
        client
            .rxRequest(HttpMethod.POST, requestURI)
            .flatMap(request -> {
                headers.forEach(request::putHeader);
                return request.rxSend(requestBody.toString());
            })
            .flatMapPublisher(response -> {
                assertThat(response.statusCode()).isEqualTo(202);
                return response.toFlowable();
            })
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertNoValues()
            .assertComplete()
            .assertNoErrors();
    }

    private static TestSubscriber<Mqtt5Publish> subscribeToMqtt5(Mqtt5RxClient mqtt5RxClient, String topic) {
        return RxJavaBridge
            .toV3Flowable(mqtt5RxClient.subscribePublishesWith().topicFilter(topic).qos(Mqtt5Publish.DEFAULT_QOS).applySubscribe())
            .take(1)
            .test();
    }

    private static void disconnectFromMqtt5(Mqtt5RxClient mqtt5RxClient) {
        RxJavaBridge.toV3Completable(mqtt5RxClient.disconnect()).blockingAwait();
    }

    private static void connectToMqtt5(VertxTestContext testContext, Mqtt5RxClient mqtt5RxClient) {
        final Checkpoint mqttConnectedCheckpoint = testContext.checkpoint();
        RxJavaBridge
            .toV3Completable(
                mqtt5RxClient
                    .connect()
                    .doOnSuccess(mqtt5ConnAck -> {
                        if (mqtt5ConnAck.getReasonCode().equals(Mqtt5ConnAckReasonCode.SUCCESS)) {
                            mqttConnectedCheckpoint.flag();
                        } else {
                            testContext.failNow("Unable to connect to MQTT5");
                        }
                    })
                    .ignoreElement()
            )
            .blockingAwait();
    }

    @NotNull
    private static Mqtt5RxClient prepareMqtt5Client() {
        return Mqtt5Client
            .builder()
            .serverHost(mqtt5.getHost())
            .serverPort(mqtt5.getMqttPort())
            .automaticReconnectWithDefaultConfig()
            .buildRx();
    }
}
