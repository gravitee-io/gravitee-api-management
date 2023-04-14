package io.gravitee.plugin.tests.messages;/**
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

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.common.http.MediaType;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.kafka.KafkaEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.get.HttpGetEntrypointConnectorFactory;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.producer.RecordMetadata;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.kafka.client.producer.KafkaProducer;
import io.vertx.rxjava3.kafka.client.producer.KafkaProducerRecord;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;
import org.testcontainers.utility.DockerImageName;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Testcontainers
@GatewayTest
@DeployApi({ "/apis/v4/messages/http-get-entrypoint-kafka-endpoint.json" })
@Disabled
class HttpGetEntrypointKafkaEndpointIntegrationTest extends AbstractGatewayTest {

    @Container
    private static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.2.1"));

    public static final String TEST_TOPIC = "test-topic";

    @Override
    public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
        endpoints.putIfAbsent("kafka", EndpointBuilder.build("kafka", KafkaEndpointConnectorFactory.class));
    }

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("http-get", EntrypointBuilder.build("http-get", HttpGetEntrypointConnectorFactory.class));
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
    void setUp() throws ExecutionException, InterruptedException, TimeoutException {
        try (
            AdminClient adminClient = AdminClient.create(
                ImmutableMap.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers())
            )
        ) {
            Collection<NewTopic> topics = List.of(new NewTopic(TEST_TOPIC, 1, (short) 1));
            adminClient.createTopics(topics).all().get(30, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("Should deploy a V4 API with a HTTP Get entrypoint and Kafka endpoint")
    void shouldBeAbleToSubscribeToKafkaEndpointWithHTTPGetEntrypoint(HttpClient client, Vertx vertx) {
        // In order to simplify the test, Kafka endpoint's consumer is configured with "autoOffsetReset": "earliest"
        // It allows us to publish the messages in the topic before opening the api connection through SSE entrypoint.
        KafkaProducer<String, byte[]> producer = getKafkaProducer(vertx);
        publishMessage(producer, "message1");
        publishMessage(producer, "message2");
        publishMessage(producer, "message3");
        producer.close();

        client
            .rxRequest(HttpMethod.GET, "/test")
            .flatMap(request -> {
                request.putHeader(HttpHeaderNames.ACCEPT.toString(), MediaType.APPLICATION_JSON);
                return request.rxSend();
            })
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return response.body();
            })
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertValue(body -> {
                final JsonObject jsonResponse = new JsonObject(body.toString());
                final JsonArray items = jsonResponse.getJsonArray("items");
                assertThat(items).hasSize(2);
                final JsonObject message = items.getJsonObject(0);
                assertThat(message.getString("content")).isEqualTo("message1");
                final JsonObject message2 = items.getJsonObject(1);
                assertThat(message2.getString("content")).isEqualTo("message2");
                return true;
            })
            .assertComplete();
    }

    /**
     * Creates a KafkaProducer to be able to publish messages to topic
     * @param vertx
     * @return
     */
    private static KafkaProducer<String, byte[]> getKafkaProducer(Vertx vertx) {
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

    private static RecordMetadata publishMessage(KafkaProducer<String, byte[]> producer, String message1) {
        return producer
            .rxSend(KafkaProducerRecord.create(TEST_TOPIC, "key", io.gravitee.gateway.api.buffer.Buffer.buffer(message1).getBytes()))
            .blockingGet();
    }
}
