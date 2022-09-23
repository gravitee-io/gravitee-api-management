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
package io.gravitee.plugin.endpoint.kafka;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.jupiter.api.ConnectorMode;
import io.gravitee.gateway.jupiter.api.connector.endpoint.async.EndpointAsyncConnector;
import io.gravitee.gateway.jupiter.api.connector.entrypoint.async.EntrypointAsyncConnector;
import io.gravitee.gateway.jupiter.api.context.ContextAttributes;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.context.InternalContextAttributes;
import io.gravitee.gateway.jupiter.api.context.MessageExecutionContext;
import io.gravitee.gateway.jupiter.api.message.DefaultMessage;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.gateway.jupiter.api.qos.Qos;
import io.gravitee.gateway.jupiter.api.qos.QosOptions;
import io.gravitee.plugin.endpoint.kafka.configuration.KafkaEndpointConnectorConfiguration;
import io.gravitee.plugin.endpoint.kafka.strategy.QosStrategy;
import io.gravitee.plugin.endpoint.kafka.strategy.QosStrategyFactory;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import reactor.adapter.rxjava.RxJava3Adapter;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;
import reactor.kafka.sender.SenderRecord;
import reactor.kafka.sender.SenderResult;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class KafkaEndpointConnector extends EndpointAsyncConnector {

    public static final Set<ConnectorMode> SUPPORTED_MODES = Set.of(ConnectorMode.PUBLISH, ConnectorMode.SUBSCRIBE);
    static final Set<Qos> SUPPORTED_QOS = Set.of(Qos.NONE, Qos.BALANCED);
    static final String KAFKA_CONTEXT_ATTRIBUTE = ContextAttributes.ATTR_PREFIX + "kafka.";
    static final String CONTEXT_ATTRIBUTE_KAFKA_TOPICS = KAFKA_CONTEXT_ATTRIBUTE + "topics";
    static final String CONTEXT_ATTRIBUTE_KAFKA_GROUP_ID = KAFKA_CONTEXT_ATTRIBUTE + "groupId";
    static final String CONTEXT_ATTRIBUTE_KAFKA_RECORD_KEY = KAFKA_CONTEXT_ATTRIBUTE + "recordKey";
    private static final String ENDPOINT_ID = "kafka";

    protected final KafkaEndpointConnectorConfiguration configuration;
    private final QosStrategyFactory qosStrategyFactory;

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
    protected Completable publish(final ExecutionContext ctx) {
        if (configuration.getProducer().isEnabled()) {
            return Completable.defer(
                () -> {
                    Map<String, Object> config = new HashMap<>();
                    config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, configuration.getBootstrapServers());
                    // Set kafka producer properties
                    config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
                    config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
                    config.put(ProducerConfig.CLIENT_ID_CONFIG, UUID.randomUUID().toString());
                    SenderOptions<String, byte[]> senderOptions = SenderOptions.create(config);
                    KafkaSender<String, byte[]> kafkaSender = KafkaSender.create(senderOptions);
                    Set<String> topics = getTopics(ctx);
                    return ctx
                        .request()
                        .onMessages(
                            upstream ->
                                RxJava3Adapter
                                    .fluxToFlowable(
                                        kafkaSender.send(
                                            upstream.flatMap(
                                                message ->
                                                    Flowable
                                                        .fromIterable(overrideTopics(topics, message))
                                                        .map(
                                                            topic -> SenderRecord.create(createProducerRecord(ctx, message, topic), message)
                                                        )
                                            )
                                        )
                                    )
                                    .map(SenderResult::correlationMetadata)
                                    .doFinally(kafkaSender::close)
                        );
                }
            );
        } else {
            return Completable.complete();
        }
    }

    private ProducerRecord<String, byte[]> createProducerRecord(
        final MessageExecutionContext ctx,
        final Message message,
        final String topic
    ) {
        ProducerRecord<String, byte[]> producerRecord = new ProducerRecord<>(topic, getKey(ctx, message), message.content().getBytes());
        if (message.headers() != null) {
            message
                .headers()
                .forEach(
                    headerEntry ->
                        producerRecord.headers().add(headerEntry.getKey(), headerEntry.getValue().getBytes(StandardCharsets.UTF_8))
                );
        }
        return producerRecord;
    }

    @Override
    protected Completable subscribe(final ExecutionContext ctx) {
        return Completable.fromRunnable(
            () -> {
                if (configuration.getConsumer().isEnabled()) {
                    ctx
                        .response()
                        .messages(
                            Flowable.defer(
                                () -> {
                                    final EntrypointAsyncConnector entrypointAsyncConnector = ctx.getInternalAttribute(
                                        InternalContextAttributes.ATTR_INTERNAL_ENTRYPOINT_CONNECTOR
                                    );
                                    final QosOptions qosOptions = entrypointAsyncConnector.qosOptions();
                                    QosStrategy qosStrategy = qosStrategyFactory.createQosStrategy(qosOptions);

                                    Map<String, Object> config = new HashMap<>();
                                    config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, configuration.getBootstrapServers());
                                    KafkaEndpointConnectorConfiguration.Consumer configurationConsumer = configuration.getConsumer();
                                    config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, configurationConsumer.getAutoOffsetReset());

                                    // Set kafka consumer properties
                                    config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
                                    config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);

                                    String groupId = getGroupId(ctx);
                                    config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
                                    String instanceId = UUID.randomUUID().toString();
                                    config.put(ConsumerConfig.GROUP_INSTANCE_ID_CONFIG, instanceId);
                                    config.put(ConsumerConfig.CLIENT_ID_CONFIG, instanceId);

                                    qosStrategy.addExtraConfig(config);

                                    ReceiverOptions<String, byte[]> receiverOptions = ReceiverOptions
                                        .<String, byte[]>create(config)
                                        .subscription(getTopics(ctx));

                                    return RxJava3Adapter.fluxToFlowable(
                                        qosStrategy
                                            .receive(ctx, configuration, receiverOptions)
                                            .map(
                                                consumerRecord -> {
                                                    HttpHeaders httpHeaders = HttpHeaders.create();
                                                    consumerRecord
                                                        .headers()
                                                        .forEach(
                                                            kafkaHeader ->
                                                                httpHeaders.add(kafkaHeader.key(), new String(kafkaHeader.value()))
                                                        );

                                                    Map<String, Object> metadata = new HashMap<>();
                                                    if (consumerRecord.key() != null) {
                                                        metadata.put("key", consumerRecord.key());
                                                    }
                                                    metadata.put("topic", consumerRecord.topic());
                                                    metadata.put("partition", consumerRecord.partition());
                                                    metadata.put("offset", consumerRecord.offset());

                                                    return DefaultMessage
                                                        .builder()
                                                        .id(qosStrategy.generateId(consumerRecord))
                                                        .headers(httpHeaders)
                                                        .content(Buffer.buffer(consumerRecord.value()))
                                                        .metadata(metadata)
                                                        .ackRunnable(qosStrategy.buildAckRunnable(consumerRecord))
                                                        .build();
                                                }
                                            )
                                    );
                                }
                            )
                        );
                }
            }
        );
    }

    private String getGroupId(final MessageExecutionContext ctx) {
        String groupId = ctx.getAttribute(CONTEXT_ATTRIBUTE_KAFKA_GROUP_ID);

        if (groupId == null || groupId.isEmpty()) {
            groupId = ctx.request().transactionId();
            if (groupId == null) {
                groupId = UUID.randomUUID().toString();
            }
            ctx.setAttribute(CONTEXT_ATTRIBUTE_KAFKA_GROUP_ID, groupId);
        }

        return groupId;
    }

    private Set<String> getTopics(final MessageExecutionContext ctx) {
        Set<String> topics = ctx.getAttribute(CONTEXT_ATTRIBUTE_KAFKA_TOPICS);
        if (topics == null || topics.isEmpty()) {
            topics = configuration.getTopics();
            ctx.setAttribute(CONTEXT_ATTRIBUTE_KAFKA_TOPICS, topics);
        }
        if (topics == null) {
            throw new IllegalStateException("Kafka topics couldn't be loaded from Configuration or Context.");
        }
        return topics;
    }

    private Set<String> overrideTopics(final Set<String> sharedTopics, final Message message) {
        Set<String> messagesTopics = message.attribute(CONTEXT_ATTRIBUTE_KAFKA_TOPICS);
        if (messagesTopics != null && !messagesTopics.isEmpty()) {
            return messagesTopics;
        }
        return sharedTopics;
    }

    private String getKey(final MessageExecutionContext ctx, final Message message) {
        String key = message.attribute(CONTEXT_ATTRIBUTE_KAFKA_RECORD_KEY);
        if (key == null) {
            key = ctx.getAttribute(CONTEXT_ATTRIBUTE_KAFKA_RECORD_KEY);
            if (key == null) {
                key = message.id();
                ctx.setAttribute(CONTEXT_ATTRIBUTE_KAFKA_RECORD_KEY, key);
            }
        }
        return key;
    }
}
