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
import com.graviteesource.reactor.message.MessageApiReactorFactory;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.fakes.MessageStorage;
import io.gravitee.apim.gateway.tests.sdk.connector.fakes.PersistentMockEndpointConnectorFactory;
import io.gravitee.apim.gateway.tests.sdk.reactor.ReactorBuilder;
import io.gravitee.apim.plugin.reactor.ReactorPlugin;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactory;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.reactivex.rxjava3.core.Completable;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.http.HttpClient;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.*;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@GatewayTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class HttpPostEntrypointMockEndpointIntegrationTest extends AbstractGatewayTest {

    @Override
    public void configureReactors(Set<ReactorPlugin<? extends ReactorFactory<?>>> reactors) {
        reactors.add(ReactorBuilder.build(MessageApiReactorFactory.class));
    }

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        super.configureEntrypoints(entrypoints);
        entrypoints.putIfAbsent("http-post", EntrypointBuilder.build("http-post", HttpPostEntrypointConnectorFactory.class));
    }

    @Override
    public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
        super.configureEndpoints(endpoints);
        endpoints.putIfAbsent("mock", EndpointBuilder.build("mock", PersistentMockEndpointConnectorFactory.class));
    }

    private MessageStorage messageStorage;

    @BeforeEach
    void setUp() {
        messageStorage = getBean(MessageStorage.class);
    }

    @AfterEach
    void tearDown() {
        messageStorage.reset();
    }

    @Test
    @DeployApi({ "/apis/v4/messages/http-post/http-post-entrypoint-mock-endpoint.json" })
    void should_publish_to_mock_endpoint(HttpClient client) {
        JsonObject requestBody = new JsonObject();
        requestBody.put("field", "value");

        postMessage(client, "/test", requestBody, Map.of("X-Test-Header", "header-value")).test().awaitDone(30, TimeUnit.SECONDS);

        messageStorage
            .subject()
            .take(1)
            .test()
            .assertValue(message -> {
                assertThat(message.headers()).isEmpty();
                assertThat(new JsonObject(message.content().toString())).isEqualTo(requestBody);
                return true;
            });
    }

    @Test
    @DeployApi({ "/apis/v4/messages/http-post/http-post-entrypoint-mock-endpoint-with-headers.json" })
    void should_publish_to_mock_endpoint_with_headers(HttpClient client) {
        JsonObject requestBody = new JsonObject();
        requestBody.put("field", "value");

        postMessage(client, "/test", requestBody, Map.of("X-Test-Header", "header-value")).test().awaitDone(30, TimeUnit.SECONDS);

        messageStorage
            .subject()
            .take(1)
            .test()
            .assertValue(message -> {
                assertThat(message.headers()).isNotEmpty();
                assertThat(message.headers().get("X-Test-Header")).isEqualTo("header-value");
                assertThat(new JsonObject(message.content().toString())).isEqualTo(requestBody);
                return true;
            });
    }

    @Test
    @DeployApi({ "/apis/v4/messages/http-post/http-post-entrypoint-mock-endpoint.json" })
    void should_publish_multiples_messages_to_mock_endpoint(HttpClient client) {
        JsonObject requestBody = new JsonObject();
        requestBody.put("field", "value");

        final int messageCount = 10;
        postMessage(client, "/test", requestBody, Map.of()).repeat(messageCount).test().awaitDone(30, TimeUnit.SECONDS);

        messageStorage.subject().take(messageCount).test().assertValueCount(messageCount);
    }

    private Completable postMessage(HttpClient client, String requestURI, JsonObject requestBody, Map<String, String> headers) {
        return client
            .rxRequest(HttpMethod.POST, requestURI)
            .flatMap(request -> {
                headers.forEach(request::putHeader);
                return request.rxSend(requestBody.toString());
            })
            .flatMapCompletable(response -> {
                assertThat(response.statusCode()).isEqualTo(202);
                return response.rxBody().ignoreElement();
            });
    }
}
