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
package io.gravitee.plugin.endpoint.kafka;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.jupiter.api.ConnectorMode;
import io.gravitee.gateway.jupiter.api.connector.endpoint.async.EndpointAsyncConnector;
import io.gravitee.gateway.jupiter.api.context.ContextAttributes;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.context.MessageExecutionContext;
import io.gravitee.gateway.jupiter.api.message.DefaultMessage;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.plugin.endpoint.kafka.configuration.KafkaEndpointConnectorConfiguration;
import io.gravitee.plugin.endpoint.kafka.vertx.client.consumer.KafkaConsumer;
import io.gravitee.plugin.endpoint.kafka.vertx.client.producer.KafkaProducer;
import io.gravitee.plugin.endpoint.kafka.vertx.client.producer.KafkaProducerRecord;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.vertx.kafka.client.common.KafkaClientOptions;
import io.vertx.reactivex.core.Vertx;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@AllArgsConstructor
public class KafkaEndpointConnector implements EndpointAsyncConnector {

    static final String KAFKA_CONTEXT_ATTRIBUTE = ContextAttributes.ATTR_PREFIX + "kafka.";
    static final String CONTEXT_ATTRIBUTE_KAFKA_TOPICS = KAFKA_CONTEXT_ATTRIBUTE + "topics";
    static final String CONTEXT_ATTRIBUTE_KAFKA_GROUP_ID = KAFKA_CONTEXT_ATTRIBUTE + "groupId";
    static final String CONTEXT_ATTRIBUTE_KAFKA_RECORD_KEY = KAFKA_CONTEXT_ATTRIBUTE + "recordKey";

    static final Set<ConnectorMode> SUPPORTED_MODES = Set.of(ConnectorMode.PUBLISH, ConnectorMode.SUBSCRIBE);

    protected final KafkaEndpointConnectorConfiguration configuration;

    @Override
    public Set<ConnectorMode> supportedModes() {
        return SUPPORTED_MODES;
    }

    @Override
    public Completable connect(final ExecutionContext ctx) {
        return prepareProducer(ctx).andThen(prepareConsumer(ctx));
    }

    private Completable prepareProducer(final MessageExecutionContext ctx) {
        if (configuration.getProducer().isEnabled()) {
            return Completable.defer(() -> {
                Map<String, String> config = new HashMap<>();
                config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, configuration.getBootstrapServers());
                // Set kafka producer properties
                config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
                config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
                config.put(ProducerConfig.CLIENT_ID_CONFIG, UUID.randomUUID().toString());
                // Classes from reactivex have been imported because of issue with classloading from parent/plugin
                KafkaProducer<String, byte[]> producer = createKafka(() -> KafkaProducer.create(getVertx(ctx), config));
                return ctx
                    .request()
                    .messages()
                    .flatMapCompletable(message ->
                        Flowable
                            .fromIterable(getTopics(ctx))
                            .flatMapCompletable(topic -> {
                                KafkaProducerRecord<String, byte[]> kafkaRecord = createKafkaRecord(ctx, message, topic);
                                return producer.rxWrite(kafkaRecord);
                            })
                    )
                    .andThen(producer.rxClose());
            });
        } else {
            return Completable.complete();
        }
    }

    private KafkaProducerRecord<String, byte[]> createKafkaRecord(
        final MessageExecutionContext ctx,
        final Message message,
        final String topic
    ) {
        KafkaProducerRecord<String, byte[]> producerRecord = KafkaProducerRecord.create(topic, getKey(ctx), message.content().getBytes());
        if (message.headers() != null) {
            message.headers().forEach(headerEntry -> producerRecord.addHeader(headerEntry.getKey(), headerEntry.getValue()));
        }
        return producerRecord;
    }

    private Completable prepareConsumer(final MessageExecutionContext ctx) {
        return Completable.fromRunnable(() -> {
            if (configuration.getConsumer().isEnabled()) {
                ctx
                    .response()
                    .messages(
                        Flowable.defer(() -> {
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
                            // Classes from reactivex have been imported because of issue while loading reactivex classes due to classloading from node/plugin separation
                            KafkaConsumer<String, byte[]> consumer = createKafka(() ->
                                KafkaConsumer.create(getVertx(ctx), new KafkaClientOptions().setConfig(config))
                            );
                            return consumer
                                .subscribe(getTopics(ctx))
                                .toFlowable()
                                .map(consumerRecord -> {
                                    HttpHeaders httpHeaders = HttpHeaders.create();
                                    consumerRecord
                                        .headers()
                                        .forEach(kafkaHeader -> httpHeaders.add(kafkaHeader.key(), kafkaHeader.value().toString()));
                                    return DefaultMessage
                                        .builder()
                                        .headers(httpHeaders)
                                        .content(Buffer.buffer(consumerRecord.value()))
                                        .metadata(
                                            Map.of(
                                                "key",
                                                consumerRecord.key(),
                                                "topic",
                                                consumerRecord.topic(),
                                                "partition",
                                                consumerRecord.partition(),
                                                "offset",
                                                consumerRecord.offset()
                                            )
                                        )
                                        .build();
                                })
                                .doFinally(consumer::close);
                        })
                    );
            }
        });
    }

    private Vertx getVertx(final MessageExecutionContext ctx) {
        return Vertx.newInstance(ctx.getComponent(io.vertx.core.Vertx.class));
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
        String topics = ctx.getAttribute(CONTEXT_ATTRIBUTE_KAFKA_TOPICS);

        if (topics == null || topics.isEmpty()) {
            topics = configuration.getTopics();
            ctx.setAttribute(CONTEXT_ATTRIBUTE_KAFKA_TOPICS, topics);
        }
        return Arrays.stream(topics.split(",")).collect(Collectors.toSet());
    }

    private String getKey(final MessageExecutionContext ctx) {
        String key = ctx.getAttribute(CONTEXT_ATTRIBUTE_KAFKA_RECORD_KEY);
        if (key == null) {
            key = UUID.randomUUID().toString();
            ctx.setAttribute(CONTEXT_ATTRIBUTE_KAFKA_RECORD_KEY, key);
        }

        return key;
    }

    /**
     * In order to properly manage class loading while dealing with kafka, we need to erase the current thread classloader by <code>null</code> to force Kafka to use plugin classloader instead of node class loader
     * See {@link org.apache.kafka.common.utils.Utils#getContextOrKafkaClassLoader}
     *
     * @param function use to create the Kafka object
     * @param <T> reference to the kafka object class
     * @return Kafka object
     */
    private <T> T createKafka(final Supplier<T> function) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        try {
            // Required to load classes from the kafka classloader
            Thread.currentThread().setContextClassLoader(null);
            return function.get();
        } finally {
            Thread.currentThread().setContextClassLoader(classLoader);
        }
    }
}
