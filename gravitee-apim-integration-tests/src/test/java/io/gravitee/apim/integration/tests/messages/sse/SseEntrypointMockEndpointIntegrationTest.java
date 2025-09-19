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
package io.gravitee.apim.integration.tests.messages.sse;

import static io.gravitee.apim.integration.tests.messages.sse.SseAssertions.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.graviteesource.entrypoint.sse.SseEntrypointConnectorFactory;
import com.graviteesource.reactor.message.MessageApiReactorFactory;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.reactor.ReactorBuilder;
import io.gravitee.apim.plugin.reactor.ReactorPlugin;
import io.gravitee.common.http.MediaType;
import io.gravitee.common.service.AbstractService;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.reactive.core.connection.ConnectionDrainManager;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactory;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.mock.MockEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.core.http.HttpClient;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@GatewayTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class SseEntrypointMockEndpointIntegrationTest extends AbstractGatewayTest {

    public static final String MESSAGE = "{ \"message\": \"hello\" }";

    @Override
    public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
        endpoints.putIfAbsent("mock", EndpointBuilder.build("mock", MockEndpointConnectorFactory.class));
    }

    @Override
    public void configureReactors(Set<ReactorPlugin<? extends ReactorFactory<?>>> reactors) {
        reactors.add(ReactorBuilder.build(MessageApiReactorFactory.class));
    }

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("sse", EntrypointBuilder.build("sse", SseEntrypointConnectorFactory.class));
    }

    @Test
    @DeployApi("/apis/v4/messages/sse/sse-entrypoint-mock-endpoint.json")
    void should_get_messages_with_default_configuration(HttpClient httpClient) {
        startSseStream(httpClient)
            .test()
            // expect 3 chunks: retry, two messages
            .awaitCount(3)
            .assertValueAt(0, chunk -> {
                assertRetry(chunk);
                return true;
            })
            .assertValueAt(1, chunk -> {
                assertOnMessage(chunk, 0L, MESSAGE);
                return true;
            })
            .assertValueAt(2, chunk -> {
                assertOnMessage(chunk, 1L, MESSAGE);
                return true;
            });
    }

    @Test
    @DeployApi("/apis/v4/messages/sse/sse-entrypoint-mock-endpoint.json")
    void should_emit_stop_message_when_connection_is_drained(HttpClient httpClient) {
        final ConnectionDrainManager connectionDrainManager = applicationContext.getBean(ConnectionDrainManager.class);

        startSseStream(httpClient)
            .doAfterNext(item -> connectionDrainManager.requestDrain())
            .lastElement()
            .test()
            // Expect the flow to finish because of the connection drain.
            .awaitDone(10, TimeUnit.SECONDS)
            .assertComplete()
            .assertValue(buffer -> buffer.toString().contains("event: goaway"));
    }

    @Test
    @DeployApi("/apis/v4/messages/sse/sse-entrypoint-with-comments-mock-endpoint.json")
    void should_get_messages_with_default_comments(HttpClient httpClient) {
        startSseStream(httpClient)
            .test()
            // expect 3 chunks: retry, two messages
            .awaitCount(3)
            .assertValueAt(0, chunk -> {
                assertRetry(chunk);
                return true;
            })
            .assertValueAt(1, chunk -> {
                assertOnMessage(chunk, 0L, MESSAGE, "greeting: mate", "type: informal");
                return true;
            })
            .assertValueAt(2, chunk -> {
                assertOnMessage(chunk, 1L, MESSAGE, "greeting: mate", "type: informal");
                return true;
            });
    }

    @Test
    @DeployApi("/apis/v4/messages/sse/sse-entrypoint-mock-endpoint-heartbeat.json")
    void should_get_message_and_heart_beat(HttpClient httpClient) {
        startSseStream(httpClient)
            .test()
            // expect 3 chunks: retry,  1 heartbeat, 1 message,
            .awaitCount(3)
            .assertValueAt(0, chunk -> {
                assertRetry(chunk);
                return true;
            })
            .assertValueAt(1, chunk -> {
                assertHeartbeat(chunk);
                return true;
            })
            .assertValueAt(2, chunk -> {
                assertOnMessage(chunk, 0L, MESSAGE);
                return true;
            });
    }

    @NotNull
    private static Flowable<Buffer> startSseStream(HttpClient httpClient) {
        return httpClient
            .rxRequest(HttpMethod.GET, "/test")
            .flatMap(request -> {
                request.putHeader(HttpHeaderNames.ACCEPT.toString(), MediaType.TEXT_EVENT_STREAM);
                return request.rxSend();
            })
            .flatMapPublisher(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return response.toFlowable();
            });
    }
}
