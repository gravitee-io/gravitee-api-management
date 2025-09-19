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

import com.graviteesource.endpoint.rabbitmq.RabbitMQEndpointConnectorFactory;
import com.graviteesource.reactor.message.MessageApiReactorFactory;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Delivery;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.reactor.ReactorBuilder;
import io.gravitee.apim.plugin.reactor.ReactorPlugin;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactory;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.adapter.rxjava.RxJava3Adapter;
import reactor.core.publisher.Flux;
import reactor.rabbitmq.BindingSpecification;
import reactor.rabbitmq.ExchangeSpecification;
import reactor.rabbitmq.OutboundMessage;
import reactor.rabbitmq.QueueSpecification;
import reactor.rabbitmq.RabbitFlux;
import reactor.rabbitmq.Receiver;
import reactor.rabbitmq.ReceiverOptions;
import reactor.rabbitmq.Sender;
import reactor.rabbitmq.SenderOptions;

@Testcontainers
@Slf4j
public abstract class AbstractRabbitMQEndpointIntegrationTest extends AbstractGatewayTest {

    protected static final String USER = "admin";
    protected static final String PASSWORD = "admin";

    @Container
    protected static final RabbitMQContainer rabbitmqContainer = new RabbitMQContainer("rabbitmq:3.11-management-alpine")
        .withUser(USER, PASSWORD, Set.of("administrator"))
        .withPermission("/", USER, ".*", ".*", ".*");

    protected String exchange;
    protected String routingKey;

    protected Sender sender;
    protected Receiver receiver;

    @Override
    public void configureReactors(Set<ReactorPlugin<? extends ReactorFactory<?>>> reactors) {
        reactors.add(ReactorBuilder.build(MessageApiReactorFactory.class));
    }

    @Override
    public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
        endpoints.putIfAbsent("rabbitmq", EndpointBuilder.build("rabbitmq", RabbitMQEndpointConnectorFactory.class));
    }

    @Override
    public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
        exchange = UUID.randomUUID().toString();
        routingKey = UUID.randomUUID().toString();

        if (definitionClass.isAssignableFrom(Api.class)) {
            Api apiDefinition = (Api) api.getDefinition();
            apiDefinition
                .getEndpointGroups()
                .stream()
                .flatMap(eg -> eg.getEndpoints().stream())
                .filter(endpoint -> endpoint.getType().equals("rabbitmq"))
                .forEach(endpoint -> {
                    endpoint.setConfiguration(
                        endpoint
                            .getConfiguration()
                            .replace("rabbitmq-host", rabbitmqContainer.getHost())
                            .replace("5672", rabbitmqContainer.getAmqpPort().toString())
                    );
                    endpoint.setSharedConfigurationOverride(
                        endpoint.getSharedConfigurationOverride().replace("my-exchange", exchange).replace("a.routing.key", routingKey)
                    );
                });
        }
    }

    @BeforeEach
    public void prepareRabbitClient() {
        initSenderAndReceiverForTest();
        log.info("RabbitMQ connected");
    }

    @AfterEach
    public void closeRabbitClient() {
        sender.close();
        receiver.close();
        log.info("RabbitMQ disconnected");
    }

    protected Flowable<Delivery> subscribeToRabbitMQ(final String exchange, String routingKey) {
        String queue = "queue-" + UUID.randomUUID();

        var exchangeSpec = ExchangeSpecification.exchange(exchange).type("topic").durable(false).autoDelete(true);
        var queueSpec = QueueSpecification.queue(queue).durable(false).autoDelete(true);
        var bindingSpec = BindingSpecification.binding().exchange(exchange).routingKey(routingKey).queue(queue);

        return RxJava3Adapter.fluxToFlowable(
            sender
                .declare(exchangeSpec)
                .flatMap(declareOk -> sender.declare(queueSpec))
                .flatMap(declareOk -> sender.bind(bindingSpec))
                .flatMapMany(bindOk -> receiver.consumeAutoAck(queue))
        );
    }

    protected Completable publishToRabbitMQ(final String exchange, String routingKey, List<String> messages) {
        return publishToRabbitMQ(exchange, routingKey, messages, 0);
    }

    protected Completable publishToRabbitMQ(final String exchange, String routingKey, List<String> messages, final long delay) {
        return publishToRabbitMQ(Flux.fromIterable(messages).map(m -> new OutboundMessage(exchange, routingKey, m.getBytes())), delay);
    }

    protected Completable publishToRabbitMQ(Flux<OutboundMessage> messages, final long delay) {
        return RxJava3Adapter.fluxToFlowable(
            sender.sendWithPublishConfirms(messages).delaySubscription(Duration.ofMillis(delay))
        ).ignoreElements();
    }

    private void initSenderAndReceiverForTest() {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(rabbitmqContainer.getHost());
        connectionFactory.setPort(rabbitmqContainer.getAmqpPort());
        connectionFactory.setUsername(USER);
        connectionFactory.setPassword(PASSWORD);

        var senderOptions = new SenderOptions();
        senderOptions.connectionFactory(connectionFactory);
        sender = RabbitFlux.createSender(senderOptions);

        var receiverOptions = new ReceiverOptions();
        receiverOptions.connectionFactory(connectionFactory);
        receiver = RabbitFlux.createReceiver(receiverOptions);
    }
}
