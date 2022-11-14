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
package io.gravitee.plugin.endpoint.mqtt5;

import com.hivemq.client.internal.mqtt.message.connect.MqttConnect;
import com.hivemq.client.internal.mqtt.message.connect.MqttConnectBuilder;
import com.hivemq.client.mqtt.exceptions.ConnectionFailedException;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.Mqtt5ClientBuilder;
import com.hivemq.client.mqtt.mqtt5.Mqtt5RxClient;
import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserProperty;
import com.hivemq.client.mqtt.mqtt5.exceptions.Mqtt5AuthException;
import com.hivemq.client.mqtt.mqtt5.exceptions.Mqtt5ConnAckException;
import com.hivemq.client.mqtt.mqtt5.exceptions.Mqtt5DisconnectException;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishBuilder;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5SubscribeBuilder;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.suback.Mqtt5SubAck;
import com.hivemq.client.rx.FlowableWithSingle;
import hu.akarnokd.rxjava3.bridge.RxJavaBridge;
import io.gravitee.common.util.Maps;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.jupiter.api.ConnectorMode;
import io.gravitee.gateway.jupiter.api.ExecutionFailure;
import io.gravitee.gateway.jupiter.api.connector.endpoint.async.EndpointAsyncConnector;
import io.gravitee.gateway.jupiter.api.context.ContextAttributes;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.message.DefaultMessage;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.gateway.jupiter.api.qos.Qos;
import io.gravitee.plugin.endpoint.mqtt5.configuration.Mqtt5EndpointConnectorConfiguration;
import io.reactivex.Maybe;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@AllArgsConstructor
public class Mqtt5EndpointConnector extends EndpointAsyncConnector {

    public static final Set<ConnectorMode> SUPPORTED_MODES = Set.of(ConnectorMode.PUBLISH, ConnectorMode.SUBSCRIBE);
    public static final Set<Qos> SUPPORTED_QOS = Set.of(Qos.NONE, Qos.BALANCED);
    static final String MQTT5_CONTEXT_ATTRIBUTE = ContextAttributes.ATTR_PREFIX + "mqtt5.";
    static final String CONTEXT_ATTRIBUTE_MQTT5_TOPIC = MQTT5_CONTEXT_ATTRIBUTE + "topic";
    static final String CONTEXT_ATTRIBUTE_MQTT5_RESPONSE_TOPIC = MQTT5_CONTEXT_ATTRIBUTE + "responseTopic";
    private static final String FAILURE_ENDPOINT_CONNECTION_FAILED = "FAILURE_ENDPOINT_CONNECTION_FAILED";
    private static final String FAILURE_ENDPOINT_CONNECTION_CLOSED = "FAILURE_ENDPOINT_CONNECTION_CLOSED";
    private static final String FAILURE_ENDPOINT_UNKNOWN_ERROR = "FAILURE_ENDPOINT_UNKNOWN_ERROR";
    private static final String FAILURE_PARAMETERS_EXCEPTION = "exception";
    private static final String ENDPOINT_ID = "mqtt5";
    private final Mqtt5EndpointConnectorConfiguration configuration;

    @Override
    public String id() {
        return ENDPOINT_ID;
    }

    @Override
    public Set<ConnectorMode> supportedModes() {
        return SUPPORTED_MODES;
    }

    @Override
    public Set<Qos> supportedQos() {
        return SUPPORTED_QOS;
    }

    protected Mqtt5EndpointConnectorConfiguration configuration() {
        return configuration;
    }

    @Override
    protected Completable subscribe(ExecutionContext ctx) {
        return Completable.fromRunnable(
            () -> {
                if (configuration.getConsumer().isEnabled()) {
                    ctx
                        .response()
                        .messages(
                            Flowable.defer(
                                () -> {
                                    Mqtt5RxClient rxClient = prepareMqtt5Client(ctx).identifier(generateSubscriberIdentifier()).buildRx();
                                    String topic = getTopic(ctx);
                                    return RxJavaBridge
                                        .toV3Flowable(
                                            rxClient
                                                .connect(prepareMqttConnect().build())
                                                .flatMapPublisher(
                                                    mqttConnAck ->
                                                        prepareMqtt5Subscribe(ctx, rxClient, topic).applySubscribe().map(this::transform)
                                                )
                                        )
                                        .doFinally(() -> closeClient(rxClient))
                                        .onErrorResumeNext(throwable -> interruptMessagesWith(ctx, throwable));
                                }
                            )
                        );
                }
            }
        );
    }

    private String generateSubscriberIdentifier() {
        return "gio-apim-subscriber-" + UUID.randomUUID().toString().split("-")[0];
    }

    protected Message transform(final Mqtt5Publish mqttPublish) {
        DefaultMessage.DefaultMessageBuilder messageBuilder = DefaultMessage.builder();

        if (mqttPublish.getPayload().isPresent()) {
            messageBuilder.content(Buffer.buffer(mqttPublish.getPayloadAsBytes()));
        }

        messageBuilder.metadata(
            Maps
                .<String, Object>builder()
                .put("topic", mqttPublish.getTopic())
                .put("type", mqttPublish.getType().name())
                .put("qos", mqttPublish.getQos().getCode())
                .put("retain", mqttPublish.isRetain())
                .put("contentType", mqttPublish.getContentType())
                .put("messageExpiryInterval", mqttPublish.getMessageExpiryInterval())
                .put("responseTopic", mqttPublish.getResponseTopic())
                .build()
        );

        return messageBuilder.build();
    }

    @Override
    protected Completable publish(ExecutionContext ctx) {
        if (configuration.getProducer().isEnabled()) {
            return Completable.defer(
                () -> {
                    Mqtt5RxClient rxClient = prepareMqtt5Client(ctx).identifier(generatePublisherIdentifier()).buildRx();
                    String topic = getTopic(ctx);
                    String responseTopic = getResponseTopic(ctx);
                    return ctx
                        .request()
                        .onMessages(
                            upstream ->
                                RxJavaBridge
                                    .toV3Flowable(
                                        rxClient
                                            .connect(prepareMqttConnect().build())
                                            .flatMapPublisher(
                                                mqttConnAckMessage ->
                                                    rxClient
                                                        .publish(
                                                            RxJavaBridge.toV2Flowable(
                                                                upstream.map(
                                                                    message ->
                                                                        prepareMqtt5Publish(ctx, topic, responseTopic, message).build()
                                                                )
                                                            )
                                                        )
                                                        .flatMapMaybe(
                                                            mqtt5PublishResult -> {
                                                                if (mqtt5PublishResult.getError().isPresent()) {
                                                                    return Maybe.error(mqtt5PublishResult.getError().get());
                                                                }
                                                                return Maybe.<Message>empty();
                                                            }
                                                        )
                                            )
                                    )
                                    .doFinally(() -> closeClient(rxClient))
                                    .onErrorResumeNext(throwable -> interruptMessagesWith(ctx, throwable))
                        );
                }
            );
        } else {
            return Completable.complete();
        }
    }

    private String generatePublisherIdentifier() {
        return "gio-apim-publisher-" + UUID.randomUUID().toString().split("-")[0];
    }

    protected Mqtt5SubscribeBuilder.Publishes.Start.Complete<FlowableWithSingle<Mqtt5Publish, Mqtt5SubAck>> prepareMqtt5Subscribe(
        final ExecutionContext ctx,
        final Mqtt5RxClient rxClient,
        final String topic
    ) {
        return rxClient.subscribePublishesWith().topicFilter(topic).qos(Mqtt5Publish.DEFAULT_QOS);
    }

    protected MqttConnectBuilder.Default prepareMqttConnect() {
        return MqttConnect.DEFAULT.extend();
    }

    protected Mqtt5ClientBuilder prepareMqtt5Client(final ExecutionContext ctx) {
        return Mqtt5Client
            .builder()
            .serverHost(configuration.getServerHost())
            .serverPort(configuration.getServerPort())
            .automaticReconnectWithDefaultConfig()
            .addDisconnectedListener(
                context -> {
                    if (context.getReconnector().getAttempts() >= configuration().getReconnectAttempts()) {
                        context.getReconnector().reconnect(false);
                    }
                }
            );
    }

    protected Mqtt5PublishBuilder.Complete prepareMqtt5Publish(
        final ExecutionContext ctx,
        final String topic,
        final String responseTopic,
        final Message message
    ) {
        Mqtt5PublishBuilder.Complete builder = Mqtt5Publish
            .builder()
            .topic(overrideConfigFromMessage(topic, message, CONTEXT_ATTRIBUTE_MQTT5_TOPIC))
            .responseTopic(overrideConfigFromMessage(responseTopic, message, CONTEXT_ATTRIBUTE_MQTT5_RESPONSE_TOPIC))
            .retain(configuration.getProducer().isRetained())
            .qos(Mqtt5Publish.DEFAULT_QOS)
            .payload(message.content().getBytes())
            .noMessageExpiry();
        if (message.headers() != null) {
            Mqtt5PublishBuilder.Complete finalBuilder = builder;
            message
                .headers()
                .forEach(header -> finalBuilder.userProperties().add(Mqtt5UserProperty.of(header.getKey(), header.getValue())));
            builder = finalBuilder.userProperties().applyUserProperties();
        }
        return builder;
    }

    private String getTopic(final ExecutionContext ctx) {
        String topic = ctx.getAttribute(CONTEXT_ATTRIBUTE_MQTT5_TOPIC);
        if (topic == null || topic.isEmpty()) {
            topic = configuration.getTopic();
            ctx.setAttribute(CONTEXT_ATTRIBUTE_MQTT5_TOPIC, topic);
        }
        if (topic == null) {
            throw new IllegalStateException("MQTT5 topics couldn't be loaded from Configuration or Context.");
        }
        return topic;
    }

    private String getResponseTopic(final ExecutionContext ctx) {
        String responseTopic = ctx.getAttribute(CONTEXT_ATTRIBUTE_MQTT5_RESPONSE_TOPIC);
        if (responseTopic == null || responseTopic.isEmpty()) {
            responseTopic = configuration.getProducer().getResponseTopic();
            if (responseTopic == null || responseTopic.isEmpty()) {
                ctx.setAttribute(CONTEXT_ATTRIBUTE_MQTT5_RESPONSE_TOPIC, responseTopic);
            }
        }
        return responseTopic;
    }

    private String overrideConfigFromMessage(final String sharedConfiguration, final Message message, final String attributeKey) {
        String returnedConfiguration = sharedConfiguration;
        String attributeConfiguration = message.attribute(attributeKey);
        if (attributeConfiguration != null && !attributeConfiguration.isEmpty()) {
            returnedConfiguration = attributeConfiguration;
        }
        if (returnedConfiguration != null) {
            message.attribute(attributeKey, returnedConfiguration);
        }
        return returnedConfiguration;
    }

    private Flowable<Message> interruptMessagesWith(final ExecutionContext ctx, final Throwable throwable) {
        if (
            throwable instanceof ConnectionFailedException ||
            throwable instanceof Mqtt5ConnAckException ||
            throwable instanceof Mqtt5AuthException
        ) {
            return ctx.interruptMessagesWith(
                new ExecutionFailure(500)
                    .message("Endpoint connection failed")
                    .key(FAILURE_ENDPOINT_CONNECTION_FAILED)
                    .parameters(Map.of(FAILURE_PARAMETERS_EXCEPTION, throwable))
            );
        } else if (throwable instanceof Mqtt5DisconnectException) {
            return ctx.interruptMessagesWith(
                new ExecutionFailure(500)
                    .message("Endpoint connection closed")
                    .key(FAILURE_ENDPOINT_CONNECTION_CLOSED)
                    .parameters(Map.of(FAILURE_PARAMETERS_EXCEPTION, throwable))
            );
        } else {
            return ctx.interruptMessagesWith(
                new ExecutionFailure(500)
                    .message("Endpoint unknown error")
                    .key(FAILURE_ENDPOINT_UNKNOWN_ERROR)
                    .parameters(Map.of(FAILURE_PARAMETERS_EXCEPTION, throwable))
            );
        }
    }

    private void closeClient(final Mqtt5RxClient client) {
        client.disconnect().onErrorComplete().subscribe();
    }
}
