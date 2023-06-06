/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.apim.integration.tests.messages.sse;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.mock.MockEndpointConnectorFactory;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.core.http.HttpClient;

import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@GatewayTest
public class SseEntrypointMockEndpointIntegrationTest extends AbstractSseGatewayTest {

    public static final String MESSAGE = "{ \"message\": \"hello\" }";

    @Override
    public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
        endpoints.putIfAbsent("mock", EndpointBuilder.build("mock", MockEndpointConnectorFactory.class));
    }

    @Test
    @DeployApi("/apis/v4/messages/sse-entrypoint-mock-endpoint.json")
    void shouldGetMessagesWithDefaultConfiguration(HttpClient httpClient) {
        startSeeStream(httpClient)
                // expect 3 chunks: retry, two messages
                .awaitCount(3)
                .assertValueAt(
                        0,
                        chunk -> {
                            assertRetry(chunk);
                            return true;
                        }
                )
                .assertValueAt(
                        1,
                        chunk -> {
                            assertOnMessage(chunk, 0L, MESSAGE);
                            return true;
                        }
                )
                .assertValueAt(
                        2,
                        chunk -> {
                            assertOnMessage(chunk, 1L, MESSAGE);
                            return true;
                        }
                );
    }

    @Test
    @DeployApi("/apis/v4/messages/sse-entrypoint-with-comments-mock-endpoint.json")
    void shouldGetMessagesWithDefaultComments(HttpClient httpClient) {
        startSeeStream(httpClient)
                // expect 3 chunks: retry, two messages
                .awaitCount(3)
                .assertValueAt(
                        0,
                        chunk -> {
                            assertRetry(chunk);
                            return true;
                        }
                )
                .assertValueAt(
                        1,
                        chunk -> {
                            assertOnMessage(chunk, 0L, MESSAGE, "X-Mock-Header: " + MESSAGE, "mock-metadata: " + MESSAGE);
                            return true;
                        }
                )
                .assertValueAt(
                        2,
                        chunk -> {
                            assertOnMessage(chunk, 1L, MESSAGE, "X-Mock-Header: " + MESSAGE, "mock-metadata: " + MESSAGE);
                            return true;
                        }
                );
    }

    @Test
    @DeployApi("/apis/v4/messages/sse-entrypoint-mock-endpoint-heartbeat.json")
    void shouldGetMessageAndHeartBeat(HttpClient httpClient) {
        startSeeStream(httpClient)
                // expect 3 chunks: retry,  1 heartbeat, 1 message,
                .awaitCount(3)
                .assertValueAt(
                        0,
                        chunk -> {
                            assertRetry(chunk);
                            return true;
                        }
                )
                .assertValueAt(
                        1,
                        chunk -> {
                            assertHeartbeat(chunk);
                            return true;
                        }
                ).assertValueAt(
                        2,
                        chunk -> {
                            assertOnMessage(chunk, 0L, MESSAGE);
                            return true;
                        }
                );
    }

    @NotNull
    private static TestSubscriber<Buffer> startSeeStream(HttpClient httpClient) {
        return httpClient
                .rxRequest(HttpMethod.GET, "/test")
                .flatMap(request -> {
                    request.putHeader(HttpHeaderNames.ACCEPT.toString(), MediaType.TEXT_EVENT_STREAM);
                    return request.rxSend();
                })
                .flatMapPublisher(response -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    return response.toFlowable();
                })
                .test();
    }

}
