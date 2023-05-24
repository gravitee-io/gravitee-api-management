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

import com.graviteesource.endpoint.mqtt5.Mqtt5EndpointConnectorFactory;
import com.graviteesource.reactor.message.MessageApiReactorFactory;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.Mqtt5RxClient;
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAckReasonCode;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishResult;
import hu.akarnokd.rxjava3.bridge.RxJavaBridge;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.reactor.ReactorBuilder;
import io.gravitee.apim.plugin.reactor.ReactorPlugin;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactory;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.reactivex.Flowable;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rxjava3.core.buffer.Buffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.hivemq.HiveMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;
import java.util.Set;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Testcontainers
public abstract class AbstractMqtt5EndpointIntegrationTest extends AbstractGatewayTest {

    @Container
    protected static final HiveMQContainer mqtt5 = new HiveMQContainer(DockerImageName.parse("hivemq/hivemq-ce").withTag("2023.3"))
            .withTmpFs(null);

    protected Mqtt5RxClient mqtt5RxClient;
    protected static final String TEST_TOPIC = "test-topic";
    protected static final String TEST_TOPIC_RETAINED = "test-topic-retained";
    protected static final String TEST_TOPIC_FAILURE = "test-topic-failure";
    protected static final String TEST_TOPIC_ATTRIBUTE = "test-topic-attribute";

    @Override
    public void configureReactors(Set<ReactorPlugin<? extends ReactorFactory<?>>> reactors) {
        reactors.add(ReactorBuilder.build(MessageApiReactorFactory.class));
    }

    @Override
    public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
        endpoints.putIfAbsent("mqtt5", EndpointBuilder.build("mqtt5", Mqtt5EndpointConnectorFactory.class));
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

    @BeforeEach
    public void beforeEach(VertxTestContext testContext) {
        mqtt5RxClient = prepareMqtt5Client();
        blockingConnectToMqtt5(testContext, mqtt5RxClient);
    }

    @AfterEach
    public void afterEach() {
        blockingDisconnectFromMqtt5(mqtt5RxClient);
    }

    protected io.reactivex.rxjava3.core.Flowable<Mqtt5PublishResult> publishToMqtt5(Mqtt5RxClient mqtt5RxClient, String topic, String payload) {
        return RxJavaBridge
                .toV3Flowable(mqtt5RxClient.publish(Flowable.just(Mqtt5Publish.builder()
                .topic(topic)
                .payload(Buffer.buffer(payload).getBytes())
                .qos(Mqtt5Publish.DEFAULT_QOS)
                .noMessageExpiry()
                .retain(true)
                .build())));
    }

    protected TestSubscriber<Mqtt5Publish> subscribeToMqtt5(Mqtt5RxClient mqtt5RxClient, String topic) {
        return RxJavaBridge
                .toV3Flowable(mqtt5RxClient.subscribePublishesWith().topicFilter(topic).qos(Mqtt5Publish.DEFAULT_QOS).applySubscribe())
                .take(1)
                .test();
    }

    protected io.reactivex.rxjava3.core.Flowable<Mqtt5Publish> subscribeToMqtt5Flowable(Mqtt5RxClient mqtt5RxClient, String topic) {
        return RxJavaBridge
                .toV3Flowable(mqtt5RxClient.subscribePublishesWith().topicFilter(topic).qos(Mqtt5Publish.DEFAULT_QOS).applySubscribe())
                .take(1);
    }

    protected void blockingDisconnectFromMqtt5(Mqtt5RxClient mqtt5RxClient) {
        disconnectFromMqtt5(mqtt5RxClient).blockingAwait();
    }

    protected Completable disconnectFromMqtt5(final Mqtt5RxClient mqtt5RxClient) {
        return RxJavaBridge.toV3Completable(mqtt5RxClient.disconnect());
    }

    protected void blockingConnectToMqtt5(VertxTestContext testContext, Mqtt5RxClient mqtt5RxClient) {
        connectToMqtt5(testContext, mqtt5RxClient)
                .blockingAwait();
    }

    protected Completable connectToMqtt5(final VertxTestContext testContext, final Mqtt5RxClient mqtt5RxClient) {
        final Checkpoint mqttConnectedCheckpoint = testContext.checkpoint();
        return RxJavaBridge
                .toV3Completable(mqtt5RxClient
                        .connect()
                        .doOnSuccess(mqtt5ConnAck -> {
                            if (mqtt5ConnAck.getReasonCode().equals(Mqtt5ConnAckReasonCode.SUCCESS)) {
                                mqttConnectedCheckpoint.flag();
                            } else {
                                testContext.failNow("Unable to connect to MQTT5");
                            }
                        })
                        .ignoreElement());
    }

    protected Mqtt5RxClient prepareMqtt5Client() {
        return Mqtt5Client
                .builder()
                .serverHost(mqtt5.getHost())
                .serverPort(mqtt5.getMqttPort())
                .automaticReconnectWithDefaultConfig()
                .buildRx();
    }

}
