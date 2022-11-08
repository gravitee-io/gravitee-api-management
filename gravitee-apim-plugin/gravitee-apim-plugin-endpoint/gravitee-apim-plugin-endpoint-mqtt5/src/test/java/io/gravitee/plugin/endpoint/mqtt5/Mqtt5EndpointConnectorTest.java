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

import static io.gravitee.gateway.jupiter.api.context.InternalContextAttributes.ATTR_INTERNAL_ENTRYPOINT_CONNECTOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.Mqtt5RxClient;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.jupiter.api.ApiType;
import io.gravitee.gateway.jupiter.api.ConnectorMode;
import io.gravitee.gateway.jupiter.api.connector.entrypoint.EntrypointConnector;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.context.Request;
import io.gravitee.gateway.jupiter.api.context.Response;
import io.gravitee.gateway.jupiter.api.message.DefaultMessage;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.plugin.endpoint.mqtt5.configuration.Mqtt5EndpointConnectorConfiguration;
import io.reactivex.Flowable;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.FlowableTransformer;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.subscribers.TestSubscriber;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.hivemq.HiveMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Testcontainers
public class Mqtt5EndpointConnectorTest {

    @Container
    private static final HiveMQContainer mqtt = new HiveMQContainer(DockerImageName.parse("hivemq/hivemq-ce:latest"));

    private final Mqtt5EndpointConnectorConfiguration configuration = new Mqtt5EndpointConnectorConfiguration();

    @Captor
    ArgumentCaptor<Flowable<Message>> messagesCaptor;

    @Captor
    ArgumentCaptor<io.reactivex.rxjava3.core.Flowable<Message>> messagesConsumerCaptor;

    @Captor
    ArgumentCaptor<FlowableTransformer<Message, Message>> messagesTransformerCaptor;

    private Mqtt5EndpointConnector mqtt5EndpointConnector;

    @Mock
    private ExecutionContext ctx;

    @Mock
    private Response response;

    @Mock
    private Request request;

    @Mock
    private EntrypointConnector entrypointConnector;

    @BeforeEach
    public void test() {
        configuration.setServerHost(mqtt.getHost());
        configuration.setServerPort(mqtt.getMqttPort());
        configuration.setIdentifier("gio-gw");
        configuration.setTopic("test/topic");
        configuration.getProducer().setRetained(true);

        mqtt5EndpointConnector = new Mqtt5EndpointConnector(configuration);

        lenient().when(ctx.response()).thenReturn(response);
        lenient().when(ctx.request()).thenReturn(request);
        lenient().when(entrypointConnector.supportedModes()).thenReturn(Set.of(ConnectorMode.SUBSCRIBE, ConnectorMode.PUBLISH));
        lenient().when(ctx.getInternalAttribute(ATTR_INTERNAL_ENTRYPOINT_CONNECTOR)).thenReturn(entrypointConnector);
    }

    @Test
    void shouldIdReturnMqtt() {
        assertThat(mqtt5EndpointConnector.id()).isEqualTo("mqtt5");
    }

    @Test
    void shouldSupportAsyncApi() {
        assertThat(mqtt5EndpointConnector.supportedApi()).isEqualTo(ApiType.ASYNC);
    }

    @Test
    void shouldSupportPublishAndSubscribeModes() {
        assertThat(mqtt5EndpointConnector.supportedModes()).containsOnly(ConnectorMode.PUBLISH, ConnectorMode.SUBSCRIBE);
    }

    @Test
    void shouldConsumeMqttMessages() throws InterruptedException {
        configuration.getProducer().setEnabled(false);

        TestObserver<Void> testObserver = mqtt5EndpointConnector.connect(ctx).test();
        testObserver.await(10, TimeUnit.SECONDS);
        testObserver.assertComplete();

        verify(response).messages(messagesConsumerCaptor.capture());
        io.reactivex.rxjava3.core.Flowable<Message> messageFlowable = messagesConsumerCaptor.getValue();
        io.reactivex.rxjava3.subscribers.TestSubscriber<Message> testSubscriber = messageFlowable.take(1).test();

        Mqtt5RxClient client = Mqtt5Client
            .builder()
            .identifier("publisher")
            .serverHost(mqtt.getHost())
            .serverPort(mqtt.getMqttPort())
            .buildRx();

        client
            .connect()
            .flatMapPublisher(
                mqtt5ConnAck ->
                    client
                        .publish(
                            Flowable.just(
                                Mqtt5Publish
                                    .builder()
                                    .topic(configuration.getTopic())
                                    .retain(configuration.getProducer().isRetained())
                                    .qos(Mqtt5Publish.DEFAULT_QOS)
                                    .payload(Buffer.buffer("message").getBytes())
                                    .build()
                            )
                        )
                        .take(1)
            )
            .ignoreElements()
            .blockingAwait();
        client.disconnect().blockingAwait();

        testSubscriber.await(10, TimeUnit.SECONDS);
        testSubscriber.assertValueCount(1);
        testSubscriber.assertValue(message -> message.content().toString().equals("message") && message.id() == null);

        client.disconnect().subscribe();
    }

    @Test
    void shouldPublishRequestMessages() throws InterruptedException {
        configuration.getConsumer().setEnabled(false);
        when(request.onMessages(any())).thenReturn(Completable.complete());

        Mqtt5RxClient client = Mqtt5Client
            .builder()
            .identifier("subscriber")
            .serverHost(mqtt.getHost())
            .serverPort(mqtt.getMqttPort())
            .buildRx();

        TestSubscriber<Mqtt5Publish> testSubscriber = client
            .connect()
            .flatMapPublisher(
                mqtt5ConnAck ->
                    client
                        .subscribePublishesWith()
                        .addSubscription()
                        .topicFilter(configuration.getTopic())
                        .qos(MqttQos.AT_LEAST_ONCE)
                        .noLocal(false)
                        .applySubscription()
                        .applySubscribe()
            )
            .doFinally(() -> client.unsubscribeWith().topicFilter(configuration.getTopic()).applyUnsubscribe())
            .take(1)
            .test();

        TestObserver<Void> testObserver = mqtt5EndpointConnector.connect(ctx).test();
        testObserver.await(10, TimeUnit.SECONDS);
        testObserver.assertComplete();

        verify(request).onMessages(messagesTransformerCaptor.capture());
        io.reactivex.rxjava3.subscribers.TestSubscriber<Message> messageTestSubscriber = io.reactivex.rxjava3.core.Flowable
            .just(DefaultMessage.builder().headers(HttpHeaders.create().add("key", "value")).content(Buffer.buffer("message")).build())
            .compose(messagesTransformerCaptor.getValue())
            .take(1)
            .test();
        messageTestSubscriber.await(10, TimeUnit.SECONDS);
        messageTestSubscriber.assertComplete();

        testSubscriber.await(10, TimeUnit.SECONDS);
        testSubscriber.assertValueCount(1);
        client.disconnect().blockingAwait();
    }
}
