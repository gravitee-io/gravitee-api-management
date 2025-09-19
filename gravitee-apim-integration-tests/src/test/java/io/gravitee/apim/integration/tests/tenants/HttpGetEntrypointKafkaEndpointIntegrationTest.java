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
package io.gravitee.apim.integration.tests.tenants;

import static org.assertj.core.api.Assertions.assertThat;

import com.graviteesource.entrypoint.http.get.HttpGetEntrypointConnectorFactory;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.integration.tests.messages.AbstractKafkaEndpointIntegrationTest;
import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpClient;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
@GatewayTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class HttpGetEntrypointKafkaEndpointIntegrationTest extends AbstractKafkaEndpointIntegrationTest {

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("http-get", EntrypointBuilder.build("http-get", HttpGetEntrypointConnectorFactory.class));
    }

    @Override
    public void configureGateway(GatewayConfigurationBuilder configurationBuilder) {
        super.configureGateway(configurationBuilder);
        configurationBuilder.set("tenant", "tenant-1");
    }

    @Test
    @DeployApi({ "/apis/v4/messages/http-get/http-get-entrypoint-kafka-endpoint-with-tenants.json" })
    void should_receive_all_messages_from_endpoint_with_tenant_1_only(HttpClient client, Vertx vertx) {
        // In order to simplify the test, Kafka endpoint's consumer is configured with "autoOffsetReset": "earliest"
        // It allows us to publish the messages in the topic before opening the api connection.
        Single.fromCallable(() -> getKafkaProducer(vertx))
            .flatMapCompletable(producer ->
                publishToKafka(producer, "test-topic-1", "message1")
                    .andThen(publishToKafka(producer, "test-topic-1", "message2"))
                    .andThen(publishToKafka(producer, "test-topic-2", "message3"))
                    .doFinally(producer::close)
            )
            .blockingAwait();

        // First request should receive 2 messages from the first endpoint.
        client
            .rxRequest(HttpMethod.GET, "/test")
            .flatMap(request -> {
                request.putHeader(HttpHeaderNames.ACCEPT.toString(), MediaType.APPLICATION_JSON);
                return request.send();
            })
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return response.body();
            })
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValue(body -> {
                final JsonObject jsonResponse = new JsonObject(body.toString());
                final JsonArray items = jsonResponse.getJsonArray("items");
                assertThat(items).hasSize(2);
                final JsonObject message = items.getJsonObject(0);
                assertThat(message.getString("id")).isNull();
                assertThat(message.getString("content")).isEqualTo("message1");
                final JsonObject message2 = items.getJsonObject(1);
                assertThat(message2.getString("id")).isNull();
                assertThat(message2.getString("content")).isEqualTo("message2");
                return true;
            });

        // The second request should not receive any messages because the tenant is not configured for the second endpoint.
        // Only the first endpoint is deployed, and all available messages have already been consumed.
        client
            .rxRequest(HttpMethod.GET, "/test")
            .flatMap(request -> {
                request.putHeader(HttpHeaderNames.ACCEPT.toString(), MediaType.APPLICATION_JSON);
                return request.send();
            })
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return response.body();
            })
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValue(body -> {
                final JsonObject jsonResponse = new JsonObject(body.toString());
                final JsonArray items = jsonResponse.getJsonArray("items");
                assertThat(items).isEmpty();
                return true;
            });
    }
}
