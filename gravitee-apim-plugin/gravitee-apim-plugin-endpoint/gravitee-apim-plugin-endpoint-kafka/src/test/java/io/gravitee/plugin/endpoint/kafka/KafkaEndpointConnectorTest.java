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

import static io.gravitee.gateway.jupiter.api.context.InternalContextAttributes.ATTR_INTERNAL_ENTRYPOINT_CONNECTOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.jupiter.api.ApiType;
import io.gravitee.gateway.jupiter.api.ConnectorMode;
import io.gravitee.gateway.jupiter.api.connector.entrypoint.async.EntrypointAsyncConnector;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.context.Request;
import io.gravitee.gateway.jupiter.api.context.Response;
import io.gravitee.gateway.jupiter.api.message.DefaultMessage;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.gateway.jupiter.api.qos.QosRequirement;
import io.gravitee.gateway.jupiter.core.context.interruption.InterruptionFailureException;
import io.gravitee.plugin.endpoint.kafka.configuration.KafkaEndpointConnectorConfiguration;
import io.gravitee.plugin.endpoint.kafka.strategy.DefaultQosStrategyFactory;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableTransformer;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import reactor.core.publisher.Flux;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.receiver.ReceiverPartition;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;
import reactor.kafka.sender.SenderRecord;
import reactor.test.StepVerifier;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@Testcontainers
class KafkaEndpointConnectorTest {

    @Container
    private static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.2.1"));

    @Captor
    ArgumentCaptor<Flowable<Message>> messagesCaptor;

    @Captor
    ArgumentCaptor<FlowableTransformer<Message, Message>> messagesTransformerCaptor;

    private KafkaEndpointConnectorConfiguration configuration;
    private KafkaEndpointConnector kafkaEndpointConnector;
    private String topicId;

    @Mock
    private ExecutionContext ctx;

    @Mock
    private Response response;

    @Mock
    private Request request;

    @Mock
    private EntrypointAsyncConnector entrypointAsyncConnector;

    @BeforeEach
    public void beforeEach() throws ExecutionException, InterruptedException, TimeoutException {
        topicId = UUID.randomUUID().toString();
        configuration = new KafkaEndpointConnectorConfiguration();
        configuration.setBootstrapServers(kafka.getBootstrapServers());
        configuration.setTopics(Set.of(topicId));
        kafkaEndpointConnector = new KafkaEndpointConnector(configuration, new DefaultQosStrategyFactory());
        lenient().when(ctx.response()).thenReturn(response);
        lenient().when(ctx.request()).thenReturn(request);
        lenient().when(entrypointAsyncConnector.supportedModes()).thenReturn(Set.of(ConnectorMode.SUBSCRIBE, ConnectorMode.PUBLISH));
        lenient().when(entrypointAsyncConnector.qosRequirement()).thenReturn(QosRequirement.builder().build());
        lenient().when(ctx.getInternalAttribute(ATTR_INTERNAL_ENTRYPOINT_CONNECTOR)).thenReturn(entrypointAsyncConnector);

        AdminClient adminClient = AdminClient.create(
            ImmutableMap.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers())
        );

        Collection<NewTopic> topics = Arrays.asList(new NewTopic(topicId, 1, (short) 1));
        adminClient.createTopics(topics).all().get(30, TimeUnit.SECONDS);
    }

    @Test
    void shouldIdReturnKafka() {
        assertThat(kafkaEndpointConnector.id()).isEqualTo("kafka");
    }

    @Test
    void shouldSupportAsyncApi() {
        assertThat(kafkaEndpointConnector.supportedApi()).isEqualTo(ApiType.ASYNC);
    }

    @Test
    void shouldSupportPublishAndSubscribeModes() {
        assertThat(kafkaEndpointConnector.supportedModes()).containsOnly(ConnectorMode.PUBLISH, ConnectorMode.SUBSCRIBE);
    }

    @Test
    void shouldPublishRequestMessages() throws InterruptedException {
        configuration.getConsumer().setEnabled(false);
        when(request.onMessages(any())).thenReturn(Completable.complete());

        TestObserver<Void> testObserver = kafkaEndpointConnector.connect(ctx).test();
        testObserver.await(10, TimeUnit.SECONDS);
        testObserver.assertComplete();
        verify(request).onMessages(messagesTransformerCaptor.capture());
        TestSubscriber<Message> messageTestSubscriber = Flowable
            .just(DefaultMessage.builder().headers(HttpHeaders.create().add("key", "value")).content(Buffer.buffer("message")).build())
            .compose(messagesTransformerCaptor.getValue())
            .test();
        messageTestSubscriber.await(10, TimeUnit.SECONDS);
        messageTestSubscriber.assertComplete();

        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, configuration.getBootstrapServers());
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());
        config.put(ConsumerConfig.CLIENT_ID_CONFIG, UUID.randomUUID().toString());
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        TopicPartition topicPartition = new TopicPartition(topicId, 0);
        ReceiverOptions<String, byte[]> receiverOptions = ReceiverOptions
            .<String, byte[]>create(config)
            .assignment(List.of(topicPartition))
            .addAssignListener(receiverPartitions -> receiverPartitions.forEach(ReceiverPartition::seekToBeginning));

        StepVerifier.create(KafkaReceiver.create(receiverOptions).receive().take(1)).expectNextCount(1).expectComplete().verify();
    }

    @Test
    void shouldConsumeKafkaMessagesWithDefaultQos() throws InterruptedException {
        configuration.getProducer().setEnabled(false);
        configuration.getConsumer().setAutoOffsetReset("earliest");

        TestObserver<Void> testObserver = kafkaEndpointConnector.connect(ctx).test();
        testObserver.await(10, TimeUnit.SECONDS);
        testObserver.assertComplete();

        verify(response).messages(messagesCaptor.capture());
        Flowable<Message> messageFlowable = messagesCaptor.getValue();

        TestSubscriber<Message> testSubscriber = messageFlowable.take(1).test();

        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, configuration.getBootstrapServers());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);

        SenderOptions<String, byte[]> senderOptions = SenderOptions.create(config);
        KafkaSender<String, byte[]> kafkaSender = KafkaSender.create(senderOptions);
        final long ingestionTimeTwoDaysAgo = Instant.now().minus(2, ChronoUnit.DAYS).toEpochMilli();
        ProducerRecord<String, byte[]> producerRecord = new ProducerRecord<>(
            topicId,
            0,
            ingestionTimeTwoDaysAgo,
            "key",
            Buffer.buffer("message").getBytes()
        );
        kafkaSender.send(Flux.just(SenderRecord.create(producerRecord, null))).blockFirst();

        testSubscriber.await(10, TimeUnit.SECONDS);
        testSubscriber.assertValueCount(1);
        testSubscriber.assertValue(
            message -> {
                assertThat(message.content()).hasToString("message");
                assertThat(message.id()).isNull();
                assertThat(message.metadata())
                    .containsKey(DefaultMessage.SOURCE_TIMESTAMP)
                    .contains(entry(DefaultMessage.SOURCE_TIMESTAMP, ingestionTimeTwoDaysAgo));
                return true;
            }
        );
    }

    @Test
    void shouldInterruptSubscribeWithInvalidConfiguration() {
        configuration.getProducer().setEnabled(false);
        configuration.getConsumer().setAutoOffsetReset("wrong");

        when(ctx.interruptMessagesWith(any()))
            .thenAnswer(invocation -> Flowable.defer(() -> Flowable.error(new InterruptionFailureException(invocation.getArgument(0)))));
        kafkaEndpointConnector.connect(ctx).test().assertComplete();

        verify(response).messages(messagesCaptor.capture());
        Flowable<Message> messageFlowable = messagesCaptor.getValue();

        messageFlowable
            .test()
            .assertError(
                throwable -> {
                    assertThat(throwable).isInstanceOf(InterruptionFailureException.class);
                    InterruptionFailureException failureException = (InterruptionFailureException) throwable;
                    assertThat(failureException.getExecutionFailure()).isNotNull();
                    assertThat(failureException.getExecutionFailure().statusCode()).isEqualTo(500);
                    assertThat(failureException.getExecutionFailure().message()).isEqualTo("Endpoint configuration invalid");
                    return true;
                }
            );
    }

    @Test
    void shouldInterruptSubscribeWithInvalidBootstrap() throws InterruptedException {
        configuration.setBootstrapServers("localhost:1");
        configuration.getProducer().setEnabled(false);

        when(ctx.interruptMessagesWith(any()))
            .thenAnswer(invocation -> Flowable.defer(() -> Flowable.error(new InterruptionFailureException(invocation.getArgument(0)))));
        kafkaEndpointConnector.connect(ctx).test().assertComplete();

        verify(response).messages(messagesCaptor.capture());
        TestSubscriber<Message> messageTestSubscriber = messagesCaptor.getValue().take(1).test();
        messageTestSubscriber.await(15, TimeUnit.SECONDS);
        messageTestSubscriber.assertError(
            throwable -> {
                assertThat(throwable).isInstanceOf(InterruptionFailureException.class);
                InterruptionFailureException failureException = (InterruptionFailureException) throwable;
                assertThat(failureException.getExecutionFailure()).isNotNull();
                assertThat(failureException.getExecutionFailure().statusCode()).isEqualTo(500);
                assertThat(failureException.getExecutionFailure().message()).isEqualTo("Endpoint connection closed");
                return true;
            }
        );
    }

    @Test
    void shouldInterruptPublishWithInvalidConfiguration() {
        configuration.getConsumer().setEnabled(false);
        configuration.setBootstrapServers("****");

        when(request.onMessages(any())).thenReturn(Completable.complete());
        when(ctx.interruptMessagesWith(any()))
            .thenAnswer(invocation -> Flowable.defer(() -> Flowable.error(new InterruptionFailureException(invocation.getArgument(0)))));

        when(request.onMessages(any())).thenReturn(Completable.complete());

        kafkaEndpointConnector.connect(ctx).test().assertComplete();

        verify(request).onMessages(messagesTransformerCaptor.capture());
        Flowable
            .just(DefaultMessage.builder().headers(HttpHeaders.create().add("key", "value")).content(Buffer.buffer("message")).build())
            .compose(messagesTransformerCaptor.getValue())
            .test()
            .assertError(
                throwable -> {
                    assertThat(throwable).isInstanceOf(InterruptionFailureException.class);
                    InterruptionFailureException failureException = (InterruptionFailureException) throwable;
                    assertThat(failureException.getExecutionFailure()).isNotNull();
                    assertThat(failureException.getExecutionFailure().statusCode()).isEqualTo(500);
                    assertThat(failureException.getExecutionFailure().message()).isEqualTo("Endpoint configuration invalid");
                    return true;
                }
            );
    }

    @Test
    void shouldInterruptPublishWithInvalidBootstrap() throws InterruptedException {
        configuration.setBootstrapServers("localhost:10");
        configuration.getConsumer().setEnabled(false);
        when(request.onMessages(any())).thenReturn(Completable.complete());

        when(ctx.interruptMessagesWith(any()))
            .thenAnswer(invocation -> Flowable.defer(() -> Flowable.error(new InterruptionFailureException(invocation.getArgument(0)))));

        kafkaEndpointConnector.connect(ctx).test().assertComplete();
        verify(request).onMessages(messagesTransformerCaptor.capture());
        TestSubscriber<Message> messageTestSubscriber = Flowable
            .just(DefaultMessage.builder().headers(HttpHeaders.create().add("key", "value")).content(Buffer.buffer("message")).build())
            .compose(messagesTransformerCaptor.getValue())
            .test();

        messageTestSubscriber.await(15, TimeUnit.SECONDS);
        messageTestSubscriber.assertError(
            throwable -> {
                assertThat(throwable).isInstanceOf(InterruptionFailureException.class);
                InterruptionFailureException failureException = (InterruptionFailureException) throwable;
                assertThat(failureException.getExecutionFailure()).isNotNull();
                assertThat(failureException.getExecutionFailure().statusCode()).isEqualTo(500);
                assertThat(failureException.getExecutionFailure().message()).isEqualTo("Endpoint connection closed");
                return true;
            }
        );
    }
}
