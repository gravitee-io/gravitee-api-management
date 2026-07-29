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
package io.gravitee.apim.integration.tests.messages.httppost;

import static org.assertj.core.api.Assertions.assertThat;

import com.graviteesource.entrypoint.http.post.HttpPostEntrypointConnectorFactory;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.apim.integration.tests.fake.InterruptMessageRequestPhasePolicy;
import io.gravitee.apim.integration.tests.messages.AbstractKafkaEndpointIntegrationTest;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.policy.assignattributes.AssignAttributesPolicy;
import io.gravitee.policy.assignattributes.configuration.AssignAttributesPolicyConfiguration;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.kafka.client.consumer.KafkaConsumer;
import io.vertx.rxjava3.kafka.client.producer.KafkaHeader;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@GatewayTest
class HttpPostEntrypointKafkaEndpointIntegrationTest extends AbstractKafkaEndpointIntegrationTest {

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("http-post", EntrypointBuilder.build("http-post", HttpPostEntrypointConnectorFactory.class));
    }

    @Override
    public void configurePolicies(Map<String, PolicyPlugin> policies) {
        policies.put(
            "interrupt-message-request-phase",
            PolicyBuilder.build("interrupt-message-request-phase", InterruptMessageRequestPhasePolicy.class)
        );
        policies.put(
            "assign-attributes",
            PolicyBuilder.build("assign-attributes", AssignAttributesPolicy.class, AssignAttributesPolicyConfiguration.class)
        );
    }

    @Test
    @DeployApi("/apis/v4/messages/http-post/http-post-entrypoint-kafka-endpoint.json")
    void should_be_able_to_publish_to_kafka_endpoint_with_httppost_entrypoint(HttpClient client, Vertx vertx) {
        JsonObject requestBody = new JsonObject();
        requestBody.put("field", "value");

        client
            .rxRequest(HttpMethod.POST, "/test")
            .flatMap(request -> {
                request.putHeader("X-Test-Header", "header-value");
                return request.rxSend(requestBody.toString());
            })
            .flatMapPublisher(response -> {
                assertThat(response.statusCode()).isEqualTo(202);
                return response.toFlowable();
            })
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertComplete()
            .assertNoErrors();

        // Configure a KafkaConsumer to read messages published on topic test-topic.
        KafkaConsumer<String, byte[]> kafkaConsumer = getKafkaConsumer(vertx);

        subscribeToKafka(kafkaConsumer)
            // We expect one message for this test
            .take(1)
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValueAt(0, message -> {
                assertThat(message.headers()).contains(KafkaHeader.header("X-Test-Header", "header-value"));
                final io.vertx.kafka.client.consumer.KafkaConsumerRecord kafkaConsumerRecord = message.getDelegate();
                assertThat(kafkaConsumerRecord.value()).isEqualTo(requestBody.toBuffer().getBytes());
                return true;
            })
            .assertComplete();

        kafkaConsumer.close().blockingAwait(30, TimeUnit.SECONDS);
    }

    @Test
    @DeployApi("/apis/v4/messages/http-post/http-post-entrypoint-kafka-endpoint-override-topic.json")
    void should_be_able_to_publish_to_kafka_topic_from_context_attribute(HttpClient client, Vertx vertx) {
        JsonObject requestBody = new JsonObject();
        requestBody.put("field", "value");

        client
            .rxRequest(HttpMethod.POST, "/test")
            .flatMap(request -> {
                request.putHeader("X-Test-Header", "header-value");
                // Override topic from header: topic is set on request phase
                request.putHeader("X-Topic-Override-Request-Level", "test-topic-attribute,another-topic-override");
                return request.rxSend(requestBody.toString());
            })
            .flatMapPublisher(response -> {
                assertThat(response.statusCode()).isEqualTo(202);
                return response.toFlowable();
            })
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertComplete()
            .assertNoErrors();

        // Configure a KafkaConsumer to read messages published on topic test-topic.
        KafkaConsumer<String, byte[]> kafkaConsumer = getKafkaConsumer(vertx);

        subscribeToKafka(kafkaConsumer, "test-topic-attribute")
            // We expect one message for this test
            .take(1)
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValueAt(0, message -> {
                assertThat(message.headers()).contains(KafkaHeader.header("X-Test-Header", "header-value"));
                final io.vertx.kafka.client.consumer.KafkaConsumerRecord kafkaConsumerRecord = message.getDelegate();
                assertThat(kafkaConsumerRecord.value()).isEqualTo(requestBody.toBuffer().getBytes());
                return true;
            })
            .assertComplete();

        subscribeToKafka(kafkaConsumer, "another-topic-override")
            // We expect one message for this test
            .take(1)
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValueAt(0, message -> {
                assertThat(message.headers()).contains(KafkaHeader.header("X-Test-Header", "header-value"));
                final io.vertx.kafka.client.consumer.KafkaConsumerRecord kafkaConsumerRecord = message.getDelegate();
                assertThat(kafkaConsumerRecord.value()).isEqualTo(requestBody.toBuffer().getBytes());
                return true;
            })
            .assertComplete();

        kafkaConsumer.close().blockingAwait(30, TimeUnit.SECONDS);
    }

    @Test
    @DeployApi("/apis/v4/messages/http-post/http-post-entrypoint-kafka-endpoint-override-topic.json")
    void should_be_able_to_publish_to_kafka_topics_from_message_attribute(HttpClient client, Vertx vertx) {
        JsonObject requestBody = new JsonObject();
        requestBody.put("field", "value");

        client
            .rxRequest(HttpMethod.POST, "/test")
            .flatMap(request -> {
                request.putHeader("X-Test-Header", "header-value");
                // Override topic from header: topic is set on request phase
                request.putHeader("X-Topic-Override-Request-Level", "test-topic-attribute");
                // Override topic from header: topic is set on message publish and override the one set on request phase
                request.putHeader("X-Topic-Override-Message-Level", "test-topic-message-attribute, another-topic-override");
                return request.rxSend(requestBody.toString());
            })
            .flatMapPublisher(response -> {
                assertThat(response.statusCode()).isEqualTo(202);
                return response.toFlowable();
            })
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertComplete()
            .assertNoErrors();

        // Configure a KafkaConsumer to read messages published on topic test-topic.
        KafkaConsumer<String, byte[]> kafkaConsumer = getKafkaConsumer(vertx);

        subscribeToKafka(kafkaConsumer, "test-topic-message-attribute")
            // We expect one message for this test
            .take(1)
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValueAt(0, message -> {
                assertThat(message.headers()).contains(KafkaHeader.header("X-Test-Header", "header-value"));
                final io.vertx.kafka.client.consumer.KafkaConsumerRecord kafkaConsumerRecord = message.getDelegate();
                assertThat(kafkaConsumerRecord.value()).isEqualTo(requestBody.toBuffer().getBytes());
                return true;
            })
            .assertComplete();

        subscribeToKafka(kafkaConsumer, "another-topic-override")
            // We expect one message for this test
            .take(1)
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValueAt(0, message -> {
                assertThat(message.headers()).contains(KafkaHeader.header("X-Test-Header", "header-value"));
                final io.vertx.kafka.client.consumer.KafkaConsumerRecord kafkaConsumerRecord = message.getDelegate();
                assertThat(kafkaConsumerRecord.value()).isEqualTo(requestBody.toBuffer().getBytes());
                return true;
            })
            .assertComplete();

        kafkaConsumer.close().blockingAwait(30, TimeUnit.SECONDS);
    }

    @Test
    @DeployApi("/apis/v4/messages/http-post/http-post-entrypoint-kafka-endpoint-failure.json")
    void should_return_an_error_when_message_failed_to_be_published(HttpClient client) {
        JsonObject requestBody = new JsonObject();
        requestBody.put("field", "value");

        client
            .rxRequest(HttpMethod.POST, "/http-post-entrypoint-kafka-endpoint-failure")
            .flatMap(request -> request.rxSend(requestBody.toString()))
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(412);
                return response.body();
            })
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertComplete()
            .assertValue(buffer -> {
                assertThat(buffer).hasToString("An error occurred");
                return true;
            });
    }
}
