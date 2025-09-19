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

import com.graviteesource.endpoint.kafka.KafkaEndpointConnectorFactory;
import com.graviteesource.reactor.message.MessageApiReactorFactory;
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
import io.vertx.core.buffer.Buffer;
import io.vertx.kafka.client.common.TopicPartition;
import io.vertx.kafka.client.serialization.BufferSerializer;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.kafka.client.consumer.KafkaConsumer;
import io.vertx.rxjava3.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.rxjava3.kafka.client.producer.KafkaProducer;
import io.vertx.rxjava3.kafka.client.producer.KafkaProducerRecord;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.provider.Arguments;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;
import org.testcontainers.utility.DockerImageName;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@Testcontainers
public abstract class AbstractKafkaEndpointIntegrationTest extends AbstractGatewayTest {

    @Container
    protected static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    protected Vertx vertx = Vertx.vertx();

    public static final String TEST_TOPIC = "test-topic";

    @Override
    public void configurePolicies(Map<String, PolicyPlugin> policies) {
        policies.put("message-flow-ready", PolicyBuilder.build("message-flow-ready", MessageFlowReadyPolicy.class));
    }

    /**
     * Provide the qos parameters:
     * <ul>
     *     <li>Qos: the gravitee Qos that is configured on the endpoint</li>
     *     <li>Expect exact range: boolean indicating if we expect to receive consecutive messages without loss (Ex: message-0, message-1, message-3)</li>
     * </ul>
     *
     * @return the test arguments.
     */
    protected Stream<Arguments> allQosParameters() {
        return Stream.of(
            Arguments.of(Qos.NONE, false),
            Arguments.of(Qos.AUTO, false),
            Arguments.of(Qos.AT_MOST_ONCE, false),
            Arguments.of(Qos.AT_LEAST_ONCE, true)
        );
    }

    @Override
    public void configureReactors(Set<ReactorPlugin<? extends ReactorFactory<?>>> reactors) {
        reactors.add(ReactorBuilder.build(MessageApiReactorFactory.class));
    }

    @Override
    public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
        endpoints.putIfAbsent("kafka", EndpointBuilder.build("kafka", KafkaEndpointConnectorFactory.class));
    }

    @Override
    public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
        if (definitionClass.isAssignableFrom(Api.class)) {
            Api apiDefinition = (Api) api.getDefinition();
            apiDefinition
                .getEndpointGroups()
                .stream()
                .flatMap(eg -> eg.getEndpoints().stream())
                .filter(endpoint -> endpoint.getType().equals("kafka"))
                .forEach(endpoint -> {
                    endpoint.setConfiguration(endpoint.getConfiguration().replace("bootstrap-server", kafka.getBootstrapServers()));
                });
        }
    }

    @BeforeEach
    void setUp() {
        try (
            AdminClient adminClient = AdminClient.create(
                ImmutableMap.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers())
            )
        ) {
            deleteTopic(adminClient, TEST_TOPIC);
            createTopic(adminClient, TEST_TOPIC);
        } catch (Exception e) {
            // Ignore this
        }
    }

    protected void deleteTopic(final AdminClient adminClient, String topic)
        throws ExecutionException, InterruptedException, TimeoutException {
        boolean deleted = false;
        adminClient.deleteTopics(Set.of(topic)).all().get(30, TimeUnit.SECONDS);
        // Because topic is actually marked as deleted, we need to ensure it is actually deleted
        while (!deleted) {
            Set<String> topics = adminClient.listTopics().names().get(30, TimeUnit.SECONDS);
            deleted = !topics.contains(topic);
        }
    }

    protected void deleteTopic(String topic) {
        try (
            AdminClient adminClient = AdminClient.create(
                ImmutableMap.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers())
            )
        ) {
            deleteTopic(adminClient, topic);
        } catch (Exception e) {
            // Ignore this
        }
    }

    protected void createTopic(final AdminClient adminClient, String topic)
        throws InterruptedException, ExecutionException, TimeoutException {
        Collection<NewTopic> topics = List.of(new NewTopic(topic, 1, (short) 1));
        adminClient.createTopics(topics).all().get(30, TimeUnit.SECONDS);
    }

    protected static KafkaConsumer<String, byte[]> getKafkaConsumer(Vertx vertx) {
        Map<String, String> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        config.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());
        config.put(ConsumerConfig.CLIENT_ID_CONFIG, UUID.randomUUID().toString());
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return KafkaConsumer.create(vertx, config);
    }

    protected static Flowable<KafkaConsumerRecord<String, byte[]>> subscribeToKafka(
        KafkaConsumer<String, byte[]> kafkaConsumer,
        String topic
    ) {
        TopicPartition topicPartition = new TopicPartition(topic, 0);
        return kafkaConsumer
            .rxAssign(topicPartition)
            .andThen(kafkaConsumer.rxSeekToBeginning(topicPartition))
            .andThen(kafkaConsumer.toFlowable());
    }

    protected static Flowable<KafkaConsumerRecord<String, byte[]>> subscribeToKafka(KafkaConsumer<String, byte[]> kafkaConsumer) {
        return subscribeToKafka(kafkaConsumer, TEST_TOPIC);
    }

    /**
     * Creates a KafkaProducer to be able to publish messages to topic
     * @param vertx
     * @return
     */
    protected static KafkaProducer<String, byte[]> getKafkaProducer(Vertx vertx) {
        Map<String, String> config = Map.of(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
            kafka.getBootstrapServers(),
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
            StringSerializer.class.getName(),
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
            ByteArraySerializer.class.getName()
        );

        return KafkaProducer.create(vertx, config);
    }

    protected static Single<KafkaProducer<String, Buffer>> getKafkaProducerSingle(Vertx vertx) {
        Map<String, String> config = Map.of(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
            kafka.getBootstrapServers(),
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
            StringSerializer.class.getName(),
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
            BufferSerializer.class.getName()
        );

        return Single.just(KafkaProducer.create(vertx, config));
    }

    protected static void blockingPublishToKafka(KafkaProducer<String, byte[]> producer, String message) {
        publishToKafka(producer, message).test().awaitDone(10, TimeUnit.SECONDS).assertComplete().assertNoErrors();
    }

    protected static Completable publishToKafka(KafkaProducer<String, byte[]> producer, String topic, String message) {
        return producer
            .rxSend(KafkaProducerRecord.create(topic, "key", io.gravitee.gateway.api.buffer.Buffer.buffer(message).getBytes()))
            .ignoreElement();
    }

    protected static Completable publishToKafka(KafkaProducer<String, byte[]> producer, String message) {
        return publishToKafka(producer, TEST_TOPIC, message);
    }

    @NonNull
    protected Completable publishMessagesWhenReady(List<Completable> readyObs, String topic) {
        return Completable.defer(() -> Completable.merge(readyObs).andThen(publishToKafka(topic, "message")));
    }

    protected Completable publishToKafka(String topic, String payload) {
        final AtomicInteger i = new AtomicInteger(0);

        return getKafkaProducerSingle(vertx)
            .flatMapCompletable(producer ->
                Single.defer(() ->
                    producer.rxSend(KafkaProducerRecord.create(topic, "key", Buffer.buffer(payload + "-" + i.getAndIncrement())))
                )
                    .delay(5, TimeUnit.MILLISECONDS)
                    .repeat()
                    .ignoreElements()
                    .doFinally(() -> {
                        log.info("Stopping publish messages");
                        producer.close(1000).blockingAwait();
                        deleteTopic(topic);
                    })
            )
            .doOnSubscribe(s -> log.info("Starting publish messages"));
    }
}
