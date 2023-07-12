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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.jupiter.api.ConnectorMode;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.context.Request;
import io.gravitee.gateway.jupiter.api.context.Response;
import io.gravitee.gateway.jupiter.api.message.DefaultMessage;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.plugin.endpoint.kafka.configuration.KafkaEndpointConnectorConfiguration;
import io.gravitee.plugin.endpoint.kafka.vertx.client.consumer.KafkaConsumer;
import io.gravitee.plugin.endpoint.kafka.vertx.client.consumer.KafkaConsumerRecord;
import io.gravitee.plugin.endpoint.kafka.vertx.client.producer.KafkaProducer;
import io.gravitee.plugin.endpoint.kafka.vertx.client.producer.KafkaProducerRecord;
import io.reactivex.Flowable;
import io.reactivex.observers.TestObserver;
import io.reactivex.subscribers.TestSubscriber;
import io.vertx.junit5.VertxExtension;
import io.vertx.reactivex.core.Vertx;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;
import org.testcontainers.utility.DockerImageName;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Testcontainers
public class KafkaEndpointConnectorTest {

    @Container
    private static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.2.1"));

    private final KafkaEndpointConnectorConfiguration configuration = new KafkaEndpointConnectorConfiguration();
    Vertx vertx = Vertx.vertx();

    @Captor
    ArgumentCaptor<Flowable<Message>> messagesCaptor;

    private KafkaEndpointConnector kafkaEndpointConnector;
    private String topicId;

    @Mock
    private ExecutionContext ctx;

    @Mock
    private Response response;

    @Mock
    private Request request;

    @BeforeEach
    public void beforeEach() throws ExecutionException, InterruptedException, TimeoutException {
        topicId = UUID.randomUUID().toString();
        configuration.setBootstrapServers(kafka.getBootstrapServers());
        configuration.setTopics(topicId);
        kafkaEndpointConnector = new KafkaEndpointConnector(configuration);
        lenient().when(ctx.getComponent(any(Class.class))).thenReturn(vertx.getDelegate());
        lenient().when(ctx.response()).thenReturn(response);
        lenient().when(ctx.request()).thenReturn(request);

        AdminClient adminClient = AdminClient.create(
            ImmutableMap.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers())
        );

        Collection<NewTopic> topics = Arrays.asList(new NewTopic(topicId, 1, (short) 1));
        adminClient.createTopics(topics).all().get(30, TimeUnit.SECONDS);
    }

    @Test
    void shouldSupportSubscribePublishMode() {
        assertThat(kafkaEndpointConnector.supportedModes()).contains(ConnectorMode.SUBSCRIBE, ConnectorMode.PUBLISH);
    }

    @Test
    void shouldPublishRequestMessages() {
        configuration.getConsumer().setEnabled(false);
        when(request.messages())
            .thenReturn(
                Flowable.just(
                    DefaultMessage.builder().headers(HttpHeaders.create().add("key", "value")).content(Buffer.buffer("message")).build()
                )
            );

        TestObserver<Void> testObserver = kafkaEndpointConnector.connect(ctx).test();
        testObserver.awaitTerminalEvent(10, TimeUnit.SECONDS);
        testObserver.assertComplete();
        verify(request).messages();

        Map<String, String> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, configuration.getBootstrapServers());
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        config.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());
        config.put(ConsumerConfig.CLIENT_ID_CONFIG, UUID.randomUUID().toString());
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        KafkaConsumer<String, byte[]> kafkaConsumer = KafkaConsumer.create(vertx, config);
        io.vertx.kafka.client.common.TopicPartition topicPartition = new io.vertx.kafka.client.common.TopicPartition(topicId, 0);
        TestSubscriber<KafkaConsumerRecord<String, byte[]>> testSubscriber = kafkaConsumer
            .assign(topicPartition)
            .rxSeekToBeginning(topicPartition)
            .andThen(kafkaConsumer.toFlowable())
            .take(1)
            .test();
        testSubscriber.awaitTerminalEvent(10, TimeUnit.SECONDS);
        testSubscriber.assertValueCount(1);
        kafkaConsumer.close();
    }

    @Test
    void shouldConsumeKafkaMessages() {
        configuration.getProducer().setEnabled(false);
        configuration.getConsumer().setAutoOffsetReset("earliest");

        TestObserver<Void> testObserver = kafkaEndpointConnector.connect(ctx).test();
        testObserver.awaitTerminalEvent(10, TimeUnit.SECONDS);
        testObserver.assertComplete();

        verify(response).messages(messagesCaptor.capture());
        Flowable<Message> messageFlowable = messagesCaptor.getValue();

        TestSubscriber<Message> testSubscriber = messageFlowable.take(1).test();

        Map<String, String> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, configuration.getBootstrapServers());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        KafkaProducer<String, byte[]> producer = KafkaProducer.create(vertx, config);
        producer.write(KafkaProducerRecord.create(topicId, "key", Buffer.buffer("message").getBytes()));
        producer.close();

        testSubscriber.awaitTerminalEvent(10, TimeUnit.SECONDS);
        testSubscriber.assertValueCount(1);
    }
}
