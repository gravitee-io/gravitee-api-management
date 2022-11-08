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

import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.Mqtt5RxClient;
import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserProperty;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishBuilder;
import hu.akarnokd.rxjava3.bridge.RxJavaBridge;
import io.gravitee.common.util.Maps;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.jupiter.api.ConnectorMode;
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
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@AllArgsConstructor
public class Mqtt5EndpointConnector extends EndpointAsyncConnector {

    static final Set<ConnectorMode> SUPPORTED_MODES = Set.of(ConnectorMode.PUBLISH, ConnectorMode.SUBSCRIBE);
    static final Set<Qos> SUPPORTED_QOS = Set.of(Qos.NONE, Qos.BALANCED);
    static final String MQTT5_CONTEXT_ATTRIBUTE = ContextAttributes.ATTR_PREFIX + "mqtt5.";
    static final String CONTEXT_ATTRIBUTE_MQTT5_TOPIC = MQTT5_CONTEXT_ATTRIBUTE + "topic";
    static final String CONTEXT_ATTRIBUTE_MQTT5_RESPONSE_TOPIC = MQTT5_CONTEXT_ATTRIBUTE + "responseTopic";
    static final String CONTEXT_ATTRIBUTE_MQTT5_IDENTIFIER = MQTT5_CONTEXT_ATTRIBUTE + "identifier";
    private static final String ENDPOINT_ID = "mqtt5";
    protected final Mqtt5EndpointConnectorConfiguration configuration;

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
                                    Mqtt5RxClient rxClient = Mqtt5Client
                                        .builder()
                                        .identifier(getIdentifier(ctx))
                                        .serverHost(configuration.getServerHost())
                                        .serverPort(configuration.getServerPort())
                                        .automaticReconnectWithDefaultConfig()
                                        .buildRx();

                                    return RxJavaBridge.toV3Flowable(
                                        rxClient
                                            .connect()
                                            .flatMapPublisher(
                                                mqttConnAck ->
                                                    rxClient
                                                        .subscribePublishesWith()
                                                        .topicFilter(getTopic(ctx))
                                                        .qos(Mqtt5Publish.DEFAULT_QOS)
                                                        .applySubscribe()
                                                        .map(
                                                            mqttPublish -> {
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
                                                                        .put(
                                                                            "messageExpiryInterval",
                                                                            mqttPublish.getMessageExpiryInterval()
                                                                        )
                                                                        .put("responseTopic", mqttPublish.getResponseTopic())
                                                                        .build()
                                                                );

                                                                return messageBuilder.build();
                                                            }
                                                        )
                                            )
                                            .doFinally(() -> closeClient(rxClient))
                                    );
                                }
                            )
                        );
                }
            }
        );
    }

    @Override
    protected Completable publish(ExecutionContext ctx) {
        if (configuration.getProducer().isEnabled()) {
            return Completable.defer(
                () -> {
                    Mqtt5RxClient rxClient = Mqtt5Client
                        .builder()
                        .identifier(getIdentifier(ctx))
                        .serverHost(configuration.getServerHost())
                        .serverPort(configuration.getServerPort())
                        .automaticReconnectWithDefaultConfig()
                        .buildRx();

                    return ctx
                        .request()
                        .onMessages(
                            upstream ->
                                RxJavaBridge
                                    .toV3Flowable(
                                        rxClient
                                            .connect()
                                            .flatMapPublisher(
                                                mqttConnAckMessage ->
                                                    rxClient
                                                        .publish(
                                                            RxJavaBridge.toV2Flowable(
                                                                upstream.map(
                                                                    message -> {
                                                                        Mqtt5PublishBuilder.Complete builder = Mqtt5Publish
                                                                            .builder()
                                                                            .topic(getTopic(ctx))
                                                                            .responseTopic(getResponseTopic(ctx))
                                                                            .retain(configuration.getProducer().isRetained())
                                                                            .qos(Mqtt5Publish.DEFAULT_QOS)
                                                                            .payload(message.content().getBytes());

                                                                        if (configuration.getProducer().getMessageExpiryInterval() > -1) {
                                                                            builder.messageExpiryInterval(
                                                                                configuration.getProducer().getMessageExpiryInterval()
                                                                            );
                                                                        } else {
                                                                            builder.noMessageExpiry();
                                                                        }
                                                                        if (message.headers() != null) {
                                                                            Mqtt5PublishBuilder.Complete finalBuilder = builder;
                                                                            message
                                                                                .headers()
                                                                                .forEach(
                                                                                    header ->
                                                                                        finalBuilder
                                                                                            .userProperties()
                                                                                            .add(
                                                                                                Mqtt5UserProperty.of(
                                                                                                    header.getKey(),
                                                                                                    header.getValue()
                                                                                                )
                                                                                            )
                                                                                );
                                                                            builder = finalBuilder.userProperties().applyUserProperties();
                                                                        }

                                                                        return builder.build();
                                                                    }
                                                                )
                                                            )
                                                        )
                                                        .flatMapMaybe(mqtt5PublishResult -> Maybe.<Message>empty())
                                            )
                                    )
                                    .doFinally(() -> closeClient(rxClient))
                        );
                }
            );
        } else {
            return Completable.complete();
        }
    }

    private String getIdentifier(ExecutionContext ctx) {
        String identifier = ctx.getAttribute(CONTEXT_ATTRIBUTE_MQTT5_IDENTIFIER);
        if (identifier == null || identifier.isEmpty()) {
            identifier = configuration.getIdentifier();
            if (identifier == null) {
                identifier = ctx.request().transactionId();
                ctx.setAttribute(CONTEXT_ATTRIBUTE_MQTT5_IDENTIFIER, identifier);
            }
        }
        return identifier;
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

    private void closeClient(final Mqtt5RxClient client) {
        client.disconnect().subscribe();
    }
}
