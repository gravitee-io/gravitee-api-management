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
package io.gravitee.apim.integration.tests.messages;

import com.graviteesource.endpoint.kafka.KafkaEndpointConnectorFactory;
import com.graviteesource.reactor.message.MessageApiReactorFactory;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.reactor.ReactorBuilder;
import io.gravitee.apim.plugin.reactor.ReactorPlugin;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactory;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.reactivex.rxjava3.core.Completable;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.kafka.client.producer.KafkaProducer;
import io.vertx.rxjava3.kafka.client.producer.KafkaProducerRecord;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;
import org.testcontainers.utility.DockerImageName;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Testcontainers
public abstract class AbstractKafkaEndpointIntegrationTest extends AbstractGatewayTest {

    @Container
    protected static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.2.1"));

    public static final String TEST_TOPIC = "test-topic";

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
            deleteTopic(adminClient);
            createTopic(adminClient);
        } catch (Exception e) {
            // Ignore this
        }
    }

    private static void deleteTopic(final AdminClient adminClient) throws ExecutionException, InterruptedException, TimeoutException {
        boolean deleted = false;
        adminClient.deleteTopics(Set.of(TEST_TOPIC)).all().get(30, TimeUnit.SECONDS);
        // Because topic is actually marked as deleted, we need to ensure it is actually deleted
        while (!deleted) {
            Set<String> topics = adminClient.listTopics().names().get(30, TimeUnit.SECONDS);
            deleted = !topics.contains(TEST_TOPIC);
        }
    }

    private static void createTopic(final AdminClient adminClient) throws InterruptedException, ExecutionException, TimeoutException {
        Collection<NewTopic> topics = List.of(new NewTopic(TEST_TOPIC, 1, (short) 1));
        adminClient.createTopics(topics).all().get(30, TimeUnit.SECONDS);
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

    protected static void blockingPublishMessage(KafkaProducer<String, byte[]> producer, String message) {
        publishMessage(producer, message).test().awaitDone(10, TimeUnit.SECONDS).assertComplete().assertNoErrors();
    }

    protected static Completable publishMessage(KafkaProducer<String, byte[]> producer, String message) {
        return producer
            .rxSend(KafkaProducerRecord.create(TEST_TOPIC, "key", io.gravitee.gateway.api.buffer.Buffer.buffer(message).getBytes()))
            .ignoreElement();
    }
}
