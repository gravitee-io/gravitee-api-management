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
package io.gravitee.apim.integration.tests.http.failover;

import static org.assertj.core.api.Assertions.assertThat;

import com.graviteesource.entrypoint.http.get.HttpGetEntrypointConnectorFactory;
import com.graviteesource.entrypoint.http.post.HttpPostEntrypointConnectorFactory;
import com.solace.messaging.publisher.DirectMessagePublisher;
import com.solace.messaging.publisher.OutboundMessage;
import com.solace.messaging.publisher.OutboundMessageBuilder;
import com.solace.messaging.resources.Topic;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.integration.tests.messages.AbstractSolaceEndpointIntegrationTest;
import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.core.http.HttpClient;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@Nested
@GatewayTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FailoverV4SolaceEndpointIntegrationTest extends AbstractSolaceEndpointIntegrationTest {

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("http-get", EntrypointBuilder.build("http-get", HttpGetEntrypointConnectorFactory.class));
        entrypoints.putIfAbsent("http-post", EntrypointBuilder.build("http-post", HttpPostEntrypointConnectorFactory.class));
    }

    @Test
    @DeployApi("/apis/v4/http/failover/solace-two-endpoints.json")
    void should_publish_on_first_retry(HttpClient client) {
        JsonObject requestBody = new JsonObject();
        requestBody.put("field", "value");

        subscribeToSolace(topic)
            .zipWith(postMessage(client, "/test", requestBody, Map.of("X-Test-Header", "header-value")).isEmpty().toFlowable(), (c, o) -> c)
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValue(message -> {
                assertThat(message.getDestinationName()).hasToString(topic);
                assertThat(message.getPayloadAsBytes()).isEqualTo(requestBody.toBuffer().getBytes());

                assertThat(message.getProperty("content-length")).isEqualTo(String.valueOf(requestBody.toString().length()));
                assertThat(message.getProperty("host")).isNotNull();
                assertThat(message.getProperty("X-Test-Header")).isEqualTo("header-value");
                assertThat(message.getProperty("X-Gravitee-Transaction-Id")).isNotNull();
                assertThat(message.getProperty("X-Gravitee-Request-Id")).isNotNull();
                return true;
            });
    }

    @Test
    @DeployApi("/apis/v4/http/failover/solace-two-endpoints.json")
    void should_subscribe_on_first_retry(HttpClient client) {
        client
            .rxRequest(HttpMethod.GET, "/test")
            .flatMap(request -> {
                request.putHeader(HttpHeaderNames.ACCEPT.toString(), MediaType.APPLICATION_JSON);
                return request.send();
            })
            .doOnSuccess(response -> assertThat(response.statusCode()).isEqualTo(200))
            .flatMap(response -> {
                final DirectMessagePublisher publisher = messagingService.createDirectMessagePublisherBuilder().build();
                return Completable.fromCompletionStage(publisher.startAsync())
                    .andThen(
                        Completable.fromRunnable(() -> {
                            Topic topic1 = Topic.of(topic);
                            OutboundMessageBuilder messageBuilder = messagingService.messageBuilder();
                            messageBuilder.withProperty("key", "value");
                            OutboundMessage outboundMessage = messageBuilder.build("message".getBytes());
                            publisher.publish(outboundMessage, topic1);
                        })
                    )
                    .andThen(response.body());
            })
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValue(body -> {
                final JsonObject jsonResponse = new JsonObject(body.toString());
                final JsonArray items = jsonResponse.getJsonArray("items");
                assertThat(items).hasSize(1);
                final JsonObject message = items.getJsonObject(0);
                assertThat(message.getString("content")).isEqualTo("message");
                return true;
            });
    }

    private Flowable<Buffer> postMessage(HttpClient client, String requestURI, JsonObject requestBody, Map<String, String> headers) {
        return client
            .rxRequest(HttpMethod.POST, requestURI)
            .flatMap(request -> {
                headers.forEach(request::putHeader);
                return request.rxSend(requestBody.toString());
            })
            .flatMapPublisher(response -> {
                assertThat(response.statusCode()).isEqualTo(202);
                return response.toFlowable();
            });
    }
}
