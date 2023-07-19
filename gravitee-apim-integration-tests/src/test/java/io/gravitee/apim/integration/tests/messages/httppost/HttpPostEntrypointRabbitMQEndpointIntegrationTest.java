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

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import com.graviteesource.entrypoint.http.post.HttpPostEntrypointConnectorFactory;
import com.rabbitmq.client.Delivery;
import com.rabbitmq.client.impl.LongStringHelper;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.apim.integration.tests.fake.InterruptMessageRequestPhasePolicy;
import io.gravitee.apim.integration.tests.messages.AbstractRabbitMQEndpointIntegrationTest;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.policy.assignattributes.AssignAttributesPolicy;
import io.gravitee.policy.assignattributes.configuration.AssignAttributesPolicyConfiguration;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.core.http.HttpClient;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

@GatewayTest
class HttpPostEntrypointRabbitMQEndpointIntegrationTest extends AbstractRabbitMQEndpointIntegrationTest {

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
    @DeployApi({ "/apis/v4/messages/http-post/http-post-entrypoint-rabbitmq-endpoint.json" })
    void should_publish_message(HttpClient client) throws InterruptedException {
        JsonObject requestBody = new JsonObject();
        requestBody.put("field", "value");

        subscribeToRabbitMQ(exchange, routingKey)
            .zipWith(
                    postMessage(client, "/test", requestBody, Map.of("X-Test-Header", "header-value"))
                        .delaySubscription(750, MILLISECONDS)
                        .isEmpty()
                        .toFlowable(),
                    (c, o) -> c
            )
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValue(message -> {
                assertThat(message.getBody()).isEqualTo(requestBody.toBuffer().getBytes());

                var headers = message.getProperties().getHeaders();
                assertThat(headers)
                    .containsEntry("X-Test-Header", LongStringHelper.asLongString("header-value"))
                    .containsKey("X-Gravitee-Transaction-Id")
                    .containsKey("X-Gravitee-Request-Id");

                return true;
            });
    }

    @Test
    @DeployApi({ "/apis/v4/messages/http-post/http-post-entrypoint-rabbitmq-endpoint-failure.json" })
    void should_return_an_error_when_message_failed_to_be_published(HttpClient client) {
        JsonObject requestBody = new JsonObject();
        requestBody.put("field", "value");

        TestSubscriber<Delivery> testSubscriber = subscribeToRabbitMQ(exchange, routingKey).test();

        client
            .rxRequest(HttpMethod.POST, "/test-failure")
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

        testSubscriber.awaitDone(2, TimeUnit.SECONDS).assertNoValues().assertNotComplete();
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
