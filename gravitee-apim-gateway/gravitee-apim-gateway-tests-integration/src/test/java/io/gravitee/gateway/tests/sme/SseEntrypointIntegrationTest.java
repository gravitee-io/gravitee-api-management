package io.gravitee.gateway.tests.sme;/**
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
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.reactivex.observers.TestObserver;
import io.reactivex.subscribers.TestSubscriber;
import io.vertx.core.http.HttpMethod;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.http.HttpClient;
import io.vertx.reactivex.core.http.HttpClientResponse;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@GatewayTest
@DeployApi({ "/apis/v4/sse-entrypoint.json" })
class SseEntrypointIntegrationTest extends AbstractGatewayTest {

    @Override
    protected void configureGateway(GatewayConfigurationBuilder gatewayConfigurationBuilder) {
        gatewayConfigurationBuilder.set("api.jupiterMode.enabled", "true");
        gatewayConfigurationBuilder.set("api.jupiterMode.default", "always");
    }

    @Test
    @DisplayName("Should deploy a V4 API with an SSE entrypoint")
    void shouldDeployV4Api(HttpClient httpClient) {
        final TestSubscriber<Buffer> obs = httpClient
            .rxRequest(HttpMethod.GET, "/test")
            .flatMap(
                request -> {
                    request.putHeader(HttpHeaderNames.ACCEPT.toString(), MediaType.TEXT_EVENT_STREAM);
                    return request.rxSend();
                }
            )
            .flatMapPublisher(HttpClientResponse::toFlowable)
            .filter(buffer -> !buffer.toString().equals(":\n\n")) // ignore heartbeat
            .test();

        // We expect 3 chunks, 1 retry message, 2 messages
        obs
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
                    assertOnChunk(chunk, 0);
                    return true;
                }
            )
            .assertValueAt(
                2,
                chunk -> {
                    assertOnChunk(chunk, 1);
                    return true;
                }
            )
            .assertNoErrors();
    }

    @Test
    @DisplayName("Should not be able to call SSE Entrypoint if no ACCEPT header")
    void shouldNotCallSseEntrypointWhenNoAcceptHeader(WebClient webClient) {
        final TestObserver<HttpResponse<Buffer>> obs = webClient.get("/test").rxSend().test();

        awaitTerminalEvent(obs)
            .assertComplete()
            .assertValue(
                response -> {
                    assertThat(response.statusCode()).isEqualTo(404);
                    return true;
                }
            );
        obs.assertNoErrors();
    }

    private void assertRetry(Buffer chunk) {
        final String[] splitMessage = chunk.toString().split("\n");
        assertThat(splitMessage).hasSize(1);
        assertThat(splitMessage[0]).startsWith("retry: ");
    }

    private void assertOnChunk(Buffer chunk, int messageNumber) {
        final String[] splitMessage = chunk.toString().split("\n");
        assertThat(splitMessage).hasSize(3);
        assertThat(splitMessage[0]).startsWith("id: " + messageNumber);
        assertThat(splitMessage[1]).isEqualTo("event: message");
        assertThat(splitMessage[2]).isEqualTo("data: Mock data");
    }
}
