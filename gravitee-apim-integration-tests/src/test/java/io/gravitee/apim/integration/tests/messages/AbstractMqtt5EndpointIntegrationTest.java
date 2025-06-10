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
package io.gravitee.apim.integration.tests.messages;

import com.graviteesource.endpoint.mqtt5.Mqtt5EndpointConnectorFactory;
import com.graviteesource.reactor.message.MessageApiReactorFactory;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.Mqtt5RxClient;
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAckReasonCode;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishResult;
import hu.akarnokd.rxjava3.bridge.RxJavaBridge;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.apim.gateway.tests.sdk.reactor.ReactorBuilder;
import io.gravitee.apim.integration.tests.fake.MessageFlowReadyPolicy;
import io.gravitee.apim.plugin.reactor.ReactorPlugin;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.gateway.reactive.api.qos.Qos;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactory;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import io.reactivex.rxjava3.subjects.Subject;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.core.http.HttpClientResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.params.provider.Arguments;
import org.testcontainers.hivemq.HiveMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@Testcontainers
public abstract class AbstractMqtt5EndpointIntegrationTest extends AbstractGatewayTest {

    protected static final String TEST_TOPIC = "test-topic";
    protected static final String TEST_TOPIC_RETAINED = "test-topic-retained";
    protected static final String TEST_TOPIC_FAILURE = "test-topic-failure";
    protected static final String TEST_TOPIC_ATTRIBUTE = "test-topic-attribute";
    protected static final Pattern MESSAGE_ID_PATTERN = Pattern.compile("message-(\\d+)", Pattern.MULTILINE);

    @Override
    public void configurePolicies(Map<String, PolicyPlugin> policies) {
        policies.put("message-flow-ready", PolicyBuilder.build("message-flow-ready", MessageFlowReadyPolicy.class));
    }

    /**
     * Provide the qos and Mqtt publish qos parameters:
     * <ul>
     *     <li>Qos: the gravitee Qos that is configured on the endpoint</li>
     *     <li>MqttQos: the Qos that is set on the published messages</li>
     *     <li>Expect exact range: boolean indicating if we expect to receive consecutive messages without loss (Ex: message-0, message-1, message-3)</li>
     * </ul>
     *
     * @return the test arguments.
     */
    protected Stream<Arguments> qosParameters() {
        return Stream.of(
            Arguments.of(Qos.NONE, MqttQos.AT_MOST_ONCE, false),
            Arguments.of(Qos.NONE, MqttQos.AT_LEAST_ONCE, false),
            Arguments.of(Qos.NONE, MqttQos.EXACTLY_ONCE, false),
            Arguments.of(Qos.AUTO, MqttQos.AT_MOST_ONCE, false),
            Arguments.of(Qos.AUTO, MqttQos.AT_LEAST_ONCE, true),
            Arguments.of(Qos.AUTO, MqttQos.EXACTLY_ONCE, true)
        );
    }

    /**
     * Provide the qos and Mqtt publish qos parameters:
     * <ul>
     *     <li>Qos: the gravitee Qos that is configured on the endpoint</li>
     *     <li>MqttQos: the Qos that is set on the published messages</li>
     *     <li>Expect exact range: boolean indicating if we expect to receive consecutive messages without loss (Ex: message-0, message-1, message-3)</li>
     * </ul>
     *
     * @return the test arguments.
     */
    protected Stream<Arguments> allQosParameters() {
        return Stream.concat(
            qosParameters(),
            Stream.of(
                Arguments.of(Qos.AT_MOST_ONCE, MqttQos.AT_MOST_ONCE, false),
                Arguments.of(Qos.AT_MOST_ONCE, MqttQos.AT_LEAST_ONCE, false),
                Arguments.of(Qos.AT_MOST_ONCE, MqttQos.EXACTLY_ONCE, false),
                Arguments.of(Qos.AT_LEAST_ONCE, MqttQos.AT_MOST_ONCE, false),
                Arguments.of(Qos.AT_LEAST_ONCE, MqttQos.AT_LEAST_ONCE, true),
                Arguments.of(Qos.AT_LEAST_ONCE, MqttQos.EXACTLY_ONCE, true)
            )
        );
    }

    @Container
    protected static final HiveMQContainer mqtt5 = new HiveMQContainer(DockerImageName.parse("hivemq/hivemq-ce").withTag("2024.4"))
        .withTmpFs(null);

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

    protected Flowable<Mqtt5PublishResult> publishToMqtt5(String topic, String payload, MqttQos publishQos) {
        final AtomicInteger i = new AtomicInteger(0);

        return connectToMqtt5()
            .flatMapPublisher(mqtt5RxClient ->
                RxJavaBridge
                    .toV3Flowable(
                        mqtt5RxClient.publish(
                            io.reactivex.Flowable
                                .fromCallable(() ->
                                    Mqtt5Publish
                                        .builder()
                                        .topic(topic)
                                        .payload(payload != null ? Buffer.buffer(payload + "-" + i.getAndIncrement()).getBytes() : null)
                                        .qos(publishQos)
                                        .noMessageExpiry()
                                        .retain(true)
                                        .build()
                                )
                                .delay(5, TimeUnit.MILLISECONDS)
                                .repeat()
                        )
                    )
                    .doFinally(() -> {
                        log.info("Stopping publish messages");
                        mqtt5RxClient.disconnect().blockingAwait();
                    })
                    .doOnSubscribe(s -> log.info("Starting publish messages"))
            );
    }

    protected Flowable<Mqtt5Publish> subscribeToMqtt5(String topic, Subject<Void> readyObs) {
        return connectToMqtt5()
            .flatMapPublisher(mqtt5RxClient ->
                RxJavaBridge
                    .toV3Flowable(mqtt5RxClient.subscribePublishesWith().topicFilter(topic).qos(Mqtt5Publish.DEFAULT_QOS).applySubscribe())
                    .doOnSubscribe(subscription -> readyObs.onComplete())
                    .doFinally(() -> mqtt5RxClient.disconnect().subscribe())
            )
            .doFinally(() -> log.info("Subscribe message completed"))
            .doOnSubscribe(s -> log.info("Subscribe message subscribed"));
    }

    protected Flowable<Mqtt5Publish> subscribeToMqtt5(String topic) {
        return subscribeToMqtt5(topic, ReplaySubject.create());
    }

    private Single<Mqtt5RxClient> connectToMqtt5() {
        final Mqtt5RxClient mqtt5RxClient = prepareMqtt5Client();
        return RxJavaBridge.toV3Single(
            mqtt5RxClient
                .connect()
                .flatMap(mqtt5ConnAck -> {
                    if (!mqtt5ConnAck.getReasonCode().equals(Mqtt5ConnAckReasonCode.SUCCESS)) {
                        return mqtt5RxClient.disconnect().andThen(io.reactivex.Single.error(new Exception("Unable to connect to MQTT5")));
                    }
                    return io.reactivex.Single.just(mqtt5RxClient);
                })
        );
    }

    protected Mqtt5RxClient prepareMqtt5Client() {
        return Mqtt5Client
            .builder()
            .serverHost(mqtt5.getHost())
            .serverPort(mqtt5.getMqttPort())
            .automaticReconnectWithDefaultConfig()
            .identifier(UUID.randomUUID().toString())
            .buildRx();
    }

    @NonNull
    protected Completable publishMessagesWhenReady(List<Completable> readyObs, String topic, MqttQos publishQos) {
        return Completable.defer(() -> Completable.merge(readyObs).andThen(publishToMqtt5(topic, "message", publishQos).ignoreElements()));
    }

    protected String extractTransactionId(HttpClientResponse response) {
        return response.getHeader("X-Gravitee-Transaction-Id");
    }
}
