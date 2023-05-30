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
package io.gravitee.apim.integration.tests.messages.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import com.graviteesource.entrypoint.http.get.HttpGetEntrypointConnectorFactory;
import com.graviteesource.reactor.message.MessageApiReactorFactory;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.reactor.ReactorBuilder;
import io.gravitee.apim.plugin.reactor.ReactorPlugin;
import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.http.HttpClient;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class MessageReactorInstalledIntegrationTest {

    @Nested
    @GatewayTest
    @DeployApi({ "/apis/v4/messages/http-get-entrypoint-mock-endpoint.json" })
    class MessageReactorPluginInstalled extends AbstractGatewayTest {

        @Override
        public void configureReactors(Set<ReactorPlugin<? extends ReactorFactory<?>>> reactors) {
            reactors.add(ReactorBuilder.build(MessageApiReactorFactory.class));
        }

        @Override
        public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
            entrypoints.putIfAbsent("http-get", EntrypointBuilder.build("http-get", HttpGetEntrypointConnectorFactory.class));
        }

        @Test
        void should_get_messages_from_mock_endpoint(HttpClient client) throws InterruptedException {
            AtomicBoolean hasJsonContent = new AtomicBoolean(false);
            client
                .rxRequest(HttpMethod.GET, "/test")
                .flatMap(request -> request.putHeader(HttpHeaderNames.ACCEPT.toString(), MediaType.APPLICATION_JSON).rxSend())
                .flatMap(response -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    assertThat(response.getHeader(HttpHeaderNames.CONTENT_TYPE)).isEqualTo(MediaType.APPLICATION_JSON);

                    // Doing content assertion only in json because it's easier
                    hasJsonContent.set(response.getHeader(HttpHeaderNames.CONTENT_TYPE).equals(MediaType.APPLICATION_JSON));
                    return response.body();
                })
                .test()
                .await()
                .assertComplete()
                .assertValue(body -> {
                    if (hasJsonContent.get()) {
                        final JsonObject content = new JsonObject(body.toString());

                        final JsonObject pagination = content.getJsonObject("pagination");
                        // This value is due to the entrypoint "messagesLimitCount" configuration field
                        assertThat(pagination.getString("nextCursor")).isEqualTo("11");

                        final JsonArray items = content.getJsonArray("items");
                        assertThat(items)
                            .hasSize(12)
                            .map(json -> Integer.parseInt(((JsonObject) json).getString("id")))
                            .containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11);
                    }
                    return true;
                })
                .assertNoErrors();
        }
    }

    @Nested
    @GatewayTest
    @DeployApi({ "/apis/v4/messages/http-get-entrypoint-mock-endpoint.json" })
    class MessageReactorPluginNotInstalled extends AbstractGatewayTest {

        @Override
        public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
            entrypoints.putIfAbsent("http-get", EntrypointBuilder.build("http-get", HttpGetEntrypointConnectorFactory.class));
        }

        @Test
        void should_not_found_api_when_no_message_reactor(HttpClient client) throws InterruptedException {
            client
                .rxRequest(HttpMethod.GET, "/test")
                .flatMap(request -> request.putHeader(HttpHeaderNames.ACCEPT.toString(), MediaType.APPLICATION_JSON).rxSend())
                .flatMap(response -> {
                    assertThat(response.statusCode()).isEqualTo(404);
                    assertThat(response.getHeader(HttpHeaderNames.CONTENT_TYPE)).isEqualTo(MediaType.TEXT_PLAIN);

                    return response.body();
                })
                .test()
                .await()
                .assertComplete()
                .assertValue(body -> {
                    assertThat(body).hasToString("No context-path matches the request URI.");
                    return true;
                })
                .assertNoErrors();
        }
    }
}
