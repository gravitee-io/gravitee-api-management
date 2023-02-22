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

import static io.gravitee.plugin.endpoint.kafka.configuration.KafkaDefaultConfiguration.RECONNECT_BACKOFF_MS;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.jupiter.api.ConnectorMode;
import io.gravitee.gateway.jupiter.api.ExecutionFailure;
import io.gravitee.gateway.jupiter.api.connector.Connector;
import io.gravitee.gateway.jupiter.api.connector.endpoint.async.EndpointAsyncConnector;
import io.gravitee.gateway.jupiter.api.connector.entrypoint.async.EntrypointAsyncConnector;
import io.gravitee.gateway.jupiter.api.context.ContextAttributes;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.context.InternalContextAttributes;
import io.gravitee.gateway.jupiter.api.context.MessageExecutionContext;
import io.gravitee.gateway.jupiter.api.message.DefaultMessage;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.gateway.jupiter.api.qos.Qos;
import io.gravitee.gateway.jupiter.api.qos.QosCapability;
import io.gravitee.gateway.jupiter.api.qos.QosRequirement;
import io.gravitee.plugin.endpoint.kafka.configuration.KafkaEndpointConnectorConfiguration;
import io.gravitee.plugin.endpoint.kafka.error.KafkaConnectionClosedException;
import io.gravitee.plugin.endpoint.kafka.error.KafkaReceiverErrorTransformer;
import io.gravitee.plugin.endpoint.kafka.error.KafkaSenderErrorTransformer;
import io.gravitee.plugin.endpoint.kafka.factory.KafkaReceiverFactory;
import io.gravitee.plugin.endpoint.kafka.factory.KafkaSenderFactory;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.errors.SaslAuthenticationException;
import org.apache.kafka.common.errors.SslAuthenticationException;
import org.apache.kafka.common.record.TimestampType;
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
@Slf4j
public class KafkaEndpointConnector extends EndpointAsyncConnector {

    public static final Set<ConnectorMode> SUPPORTED_MODES = Set.of(ConnectorMode.PUBLISH, ConnectorMode.SUBSCRIBE);
    static final Set<Qos> SUPPORTED_QOS = Set.of(Qos.NONE, Qos.AUTO);
    static final String KAFKA_CONTEXT_ATTRIBUTE = ContextAttributes.ATTR_PREFIX + "kafka.";
    static final String CONTEXT_ATTRIBUTE_KAFKA_TOPICS = KAFKA_CONTEXT_ATTRIBUTE + "topics";
    static final String CONTEXT_ATTRIBUTE_KAFKA_GROUP_ID = KAFKA_CONTEXT_ATTRIBUTE + "groupId";
    static final String CONTEXT_ATTRIBUTE_KAFKA_RECORD_KEY = KAFKA_CONTEXT_ATTRIBUTE + "recordKey";
    private static final Set<QosCapability> SUPPORTED_QOS_CAPABILITIES = Set.of(QosCapability.AUTO_ACK);
    private static final String FAILURE_ENDPOINT_CONNECTION_CLOSED = "FAILURE_ENDPOINT_CONNECTION_CLOSED";
    private static final String FAILURE_ENDPOINT_CONFIGURATION_INVALID = "FAILURE_ENDPOINT_CONFIGURATION_INVALID";
    private static final String FAILURE_ENDPOINT_UNKNOWN_ERROR = "FAILURE_ENDPOINT_UNKNOWN_ERROR";
    private static final String FAILURE_PARAMETERS_EXCEPTION = "exception";
    private static final String ENDPOINT_ID = "kafka";
    protected final KafkaEndpointConnectorConfiguration configuration;
    private final QosStrategyFactory qosStrategyFactory;

    private final KafkaReceiverFactory kafkaReceiverFactory = new KafkaReceiverFactory();
    private final KafkaSenderFactory kafkaSenderFactory = new KafkaSenderFactory();

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
    public Set<QosCapability> supportedQosCapabilities() {
        return SUPPORTED_QOS_CAPABILITIES;
    }

    @Override
    public Completable publish(final ExecutionContext ctx) {
        if (configuration.getProducer().isEnabled()) {
            return Completable.defer(
                () -> {
                    Map<String, Object> config = new HashMap<>();
                    config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, configuration.getBootstrapServers());
                    // Set kafka producer properties
                    config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
                    config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
                    config.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, RECONNECT_BACKOFF_MS);
                    addCustomProducerConfig(config);
                    SenderOptions<String, byte[]> senderOptions = SenderOptions.create(config);
                    KafkaSender<String, byte[]> kafkaSender = kafkaSenderFactory.createSender(senderOptions);
                    Set<String> topics = getTopics(ctx);
                    return ctx
                        .request()
                        .onMessages(
                            upstream ->
                                RxJava3Adapter
                                    .fluxToFlowable(
                                        kafkaSender
                                            .send(
                                                upstream.flatMap(
                                                    message ->
                                                        Flowable
                                                            .fromIterable(overrideTopics(topics, message))
                                                            .map(
                                                                topic ->
                                                                    SenderRecord.create(createProducerRecord(ctx, message, topic), message)
                                                            )
                                                )
                                            )
                                            .transform(KafkaSenderErrorTransformer.transform(kafkaSender))
                                            .map(SenderResult::correlationMetadata)
                                    )
                                    .onErrorResumeNext(throwable -> interruptMessagesWith(ctx, throwable))
                        );
                }
            );
        } else {
            return Completable.complete();
        }
    }

    /**
     * This method could be overridden to add custom configuration for producer client.
     * @param config
     */
    protected void addCustomProducerConfig(final Map<String, Object> config) {
        // Nothing to do here
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
    public Completable subscribe(final ExecutionContext ctx) {
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
                                    final QosRequirement qosRequirement = entrypointAsyncConnector.qosRequirement();
                                    QosStrategy<String, byte[]> qosStrategy = qosStrategyFactory.createQosStrategy(
                                        kafkaReceiverFactory,
                                        qosRequirement
                                    );

                                    Map<String, Object> config = new HashMap<>();
                                    config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, configuration.getBootstrapServers());
                                    config.put(ConsumerConfig.RECONNECT_BACKOFF_MS_CONFIG, RECONNECT_BACKOFF_MS);
                                    KafkaEndpointConnectorConfiguration.Consumer configurationConsumer = configuration.getConsumer();
                                    config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, configurationConsumer.getAutoOffsetReset());

                                    // Set kafka consumer properties
                                    config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
                                    config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);

                                    String groupId = getGroupId(ctx);
                                    config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

                                    addCustomConsumerConfig(config);
                                    qosStrategy.addCustomConfig(config);

                                    ReceiverOptions<String, byte[]> receiverOptions = ReceiverOptions
                                        .<String, byte[]>create(config)
                                        .subscription(getTopics(ctx));

                                    return RxJava3Adapter
                                        .<Message>fluxToFlowable(
                                            qosStrategy
                                                .receive(ctx, configuration, receiverOptions)
                                                .transform(KafkaReceiverErrorTransformer.transform(qosStrategy))
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

                                                        final DefaultMessage.DefaultMessageBuilder messageBuilder = DefaultMessage
                                                            .builder()
                                                            .id(qosStrategy.generateId(consumerRecord))
                                                            .headers(httpHeaders)
                                                            .content(Buffer.buffer(consumerRecord.value()))
                                                            .metadata(metadata)
                                                            .ackRunnable(qosStrategy.buildAckRunnable(consumerRecord));
                                                        if (
                                                            consumerRecord.timestampType() == TimestampType.CREATE_TIME ||
                                                            consumerRecord.timestampType() == TimestampType.LOG_APPEND_TIME
                                                        ) {
                                                            messageBuilder.sourceTimestamp(consumerRecord.timestamp());
                                                        }
                                                        return messageBuilder.build();
                                                    }
                                                )
                                        )
                                        .onErrorResumeNext(throwable -> interruptMessagesWith(ctx, throwable));
                                }
                            )
                        );
                }
            }
        );
    }

    private Flowable<Message> interruptMessagesWith(final ExecutionContext ctx, final Throwable throwable) {
        if (throwable instanceof KafkaConnectionClosedException) {
            return ctx.interruptMessagesWith(
                new ExecutionFailure(500)
                    .message("Endpoint connection closed")
                    .key(FAILURE_ENDPOINT_CONNECTION_CLOSED)
                    .parameters(Map.of(FAILURE_PARAMETERS_EXCEPTION, throwable))
            );
        } else if (
            throwable instanceof ConfigException ||
            throwable instanceof SaslAuthenticationException ||
            throwable instanceof SslAuthenticationException
        ) {
            return ctx.interruptMessagesWith(
                new ExecutionFailure(500)
                    .message("Endpoint configuration invalid")
                    .key(FAILURE_ENDPOINT_CONFIGURATION_INVALID)
                    .parameters(Map.of(FAILURE_PARAMETERS_EXCEPTION, throwable))
            );
        } else if (throwable instanceof KafkaException || throwable.getCause() != null) {
            return interruptMessagesWith(ctx, throwable.getCause());
        } else {
            return ctx.interruptMessagesWith(
                new ExecutionFailure(500)
                    .message("Endpoint unknown error")
                    .key(FAILURE_ENDPOINT_UNKNOWN_ERROR)
                    .parameters(Map.of(FAILURE_PARAMETERS_EXCEPTION, throwable))
            );
        }
    }

    /**
     * This method could be overridden to add custom configuration for consumer client
     * @param config
     */
    protected void addCustomConsumerConfig(final Map<String, Object> config) {
        // Nothing to do here
    }

    private String getGroupId(final MessageExecutionContext ctx) {
        String groupId = ctx.getAttribute(CONTEXT_ATTRIBUTE_KAFKA_GROUP_ID);

        if (groupId == null || groupId.isEmpty()) {
            groupId = ctx.request().clientIdentifier();
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
            }
        }
        if (key != null) {
            message.attribute(CONTEXT_ATTRIBUTE_KAFKA_RECORD_KEY, key);
        }
        return key;
    }

    @Override
    public Connector stop() throws Exception {
        super.stop();
        kafkaSenderFactory.clear();
        kafkaReceiverFactory.clear();
        return this;
    }
}
