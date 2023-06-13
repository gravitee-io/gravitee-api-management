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

import static org.assertj.core.api.Assertions.assertThat;

import com.graviteesource.entrypoint.http.get.HttpGetEntrypointConnectorFactory;
import com.graviteesource.entrypoint.http.post.HttpPostEntrypointConnectorFactory;
import com.graviteesource.entrypoint.sse.SseEntrypointConnectorFactory;
import com.graviteesource.entrypoint.websocket.WebSocketEntrypointConnectorFactory;
import com.graviteesource.reactor.message.MessageApiReactorFactory;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.apim.gateway.tests.sdk.reactor.ReactorBuilder;
import io.gravitee.apim.integration.tests.fake.ConnectorToHeaderPolicy;
import io.gravitee.apim.plugin.reactor.ReactorPlugin;
import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.plugin.resource.ResourcePlugin;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.http.HttpClient;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * The goal of this test is to configure a message api with all the entrypoints, and verify the correct entrypoint is used when doing some request.
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Testcontainers
@GatewayTest
@DeployApi({ "/apis/v4/messages/all-entrypoints-mock-endpoint.json" })
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class EntrypointSelectionIntegrationTest extends AbstractGatewayTest {

    @Override
    public void configureReactors(Set<ReactorPlugin<? extends ReactorFactory<?>>> reactors) {
        reactors.add(ReactorBuilder.build(MessageApiReactorFactory.class));
    }

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("http-post", EntrypointBuilder.build("http-post", HttpPostEntrypointConnectorFactory.class));
        entrypoints.putIfAbsent("http-get", EntrypointBuilder.build("http-get", HttpGetEntrypointConnectorFactory.class));
        entrypoints.putIfAbsent("sse", EntrypointBuilder.build("sse", SseEntrypointConnectorFactory.class));
        entrypoints.putIfAbsent("websocket", EntrypointBuilder.build("websocket", WebSocketEntrypointConnectorFactory.class));
    }

    @Override
    public void configureResources(Map<String, ResourcePlugin> resources) {
        super.configureResources(resources);
    }

    @Override
    public void configurePolicies(Map<String, PolicyPlugin> policies) {
        policies.put(ConnectorToHeaderPolicy.ID, PolicyBuilder.build(ConnectorToHeaderPolicy.ID, ConnectorToHeaderPolicy.class));
    }

    @Test
    void should_select_httpget_entrypoint_on_get_request(HttpClient client) {
        client
            .rxRequest(HttpMethod.GET, "/test")
            .flatMap(request -> request.putHeader(HttpHeaderNames.ACCEPT.toString(), MediaType.APPLICATION_JSON).rxSend())
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(response.headers().get("X-Endpoint-Used")).isEqualTo("mock");
                assertThat(response.headers().get("X-Entrypoint-Used")).isEqualTo("http-get");
                assertThat(response.getHeader(HttpHeaderNames.CONTENT_TYPE)).isEqualTo(MediaType.APPLICATION_JSON);
                return response.body();
            })
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertValue(body -> {
                final JsonObject content = new JsonObject(body.toString());

                final JsonObject pagination = content.getJsonObject("pagination");
                assertThat(pagination.getString("cursor")).isNull();
                // This value is due to the entrypoint "messagesLimitCount" configuration field
                assertThat(pagination.getString("nextCursor")).isEqualTo("11");

                final JsonArray items = content.getJsonArray("items");
                assertThat(items)
                    .hasSize(12)
                    .map(json -> Integer.parseInt(((JsonObject) json).getString("id")))
                    .containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11);
                return true;
            })
            .assertComplete()
            .assertNoErrors();
    }

    @Test
    void should_select_httppost_entrypoint_on_post_request(HttpClient client) {
        final String messageContent = "This is the message content!";
        client
            .rxRequest(HttpMethod.POST, "/test")
            .flatMap(request -> request.rxSend(messageContent))
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(202);
                assertThat(response.headers().get("X-Endpoint-Used")).isEqualTo("mock");
                assertThat(response.headers().get("X-Entrypoint-Used")).isEqualTo("http-post");
                return response.body();
            })
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertValue(body -> {
                assertThat(body).hasToString("");
                return true;
            })
            .assertComplete()
            .assertNoErrors();
    }

    @Test
    void should_select_sse_entrypoint_on_request_matching_sse(HttpClient client) {
        client
            .rxRequest(HttpMethod.GET, "/test")
            .flatMap(request -> request.putHeader(HttpHeaderNames.ACCEPT.toString(), MediaType.TEXT_EVENT_STREAM).rxSend())
            .flatMapPublisher(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(response.headers().get("X-Endpoint-Used")).isEqualTo("mock");
                assertThat(response.headers().get("X-Entrypoint-Used")).isEqualTo("sse");
                return response.toFlowable();
            })
            .test()
            .awaitDone(15, TimeUnit.SECONDS)
            // First message is a retry message, then the 15 messages configured for the mock endpoint are sent.
            .assertValueCount(16)
            .assertNoErrors()
            .cancel();
    }

    @Test
    void should_select_websocket_entrypoint_on_websocket_request(HttpClient client) {
        client
            .rxWebSocket("/test")
            .flatMapPublisher(response -> {
                assertThat(response.headers().contains("sec-websocket-accept")).isTrue();
                return response.toFlowable();
            })
            .test()
            .awaitCount(15)
            .assertNoErrors()
            .cancel();
    }
}
