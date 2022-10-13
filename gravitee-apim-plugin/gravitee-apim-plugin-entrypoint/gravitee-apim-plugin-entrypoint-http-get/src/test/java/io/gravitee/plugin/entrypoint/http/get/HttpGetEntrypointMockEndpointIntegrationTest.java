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
package io.gravitee.plugin.entrypoint.http.get;

import static io.gravitee.plugin.entrypoint.http.get.HttpGetEntrypointConnector.CURSOR_QUERY_PARAM;
import static io.gravitee.plugin.entrypoint.http.get.HttpGetEntrypointConnector.LIMIT_QUERY_PARAM;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.reactivex.observers.TestObserver;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.client.HttpRequest;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@GatewayTest
@DeployApi({ "/apis/http-get-entrypoint.json" })
class HttpGetEntrypointMockEndpointIntegrationTest extends AbstractGatewayTest {

    public static final String TEST_TOPIC = "test-topic";

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("http-get", EntrypointBuilder.build("http-get", HttpGetEntrypointConnectorFactory.class));
    }

    @Override
    protected void configureGateway(GatewayConfigurationBuilder gatewayConfigurationBuilder) {
        gatewayConfigurationBuilder.set("api.jupiterMode.enabled", "true");
        gatewayConfigurationBuilder.set("api.jupiterMode.default", "always");
    }

    @ParameterizedTest(name = "Expected: {0}, parameters: {1}")
    @MethodSource("io.gravitee.plugin.entrypoint.http.get.utils.IntegrationTestMethodSourceProvider#provideValidAcceptHeaders")
    @DisplayName("Should deploy a V4 API with a HTTP GET entrypoint and get the good Content-Type for response")
    void shouldGetCorrectContentType(String expectedHeader, List<String> acceptHeaderValues, WebClient client) {
        final HttpRequest<Buffer> request = client.get("/test");
        request.putHeader(HttpHeaderNames.ACCEPT.toString(), acceptHeaderValues);
        final TestObserver<HttpResponse<Buffer>> obs = request.rxSend().test();

        awaitTerminalEvent(obs)
            .assertComplete()
            .assertValue(
                response -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    assertThat(response.getHeader(HttpHeaderNames.CONTENT_TYPE)).isEqualTo(expectedHeader);

                    // Doing content assertion only in json because it's easier
                    if (response.getHeader(HttpHeaderNames.CONTENT_TYPE).equals(MediaType.APPLICATION_JSON)) {
                        final JsonObject content = response.bodyAsJsonObject();

                        final JsonObject pagination = content.getJsonObject("pagination");
                        assertThat(pagination.getString(CURSOR_QUERY_PARAM)).isNull();
                        // This value is due to the entrypoint "messagesLimitCount" configuration field
                        assertThat(pagination.getString("nextCursor")).isEqualTo("11");

                        final JsonArray items = content.getJsonArray("items");
                        assertThat(items)
                            .hasSize(12)
                            .map(json -> Integer.parseInt(((JsonObject) json).getString("id")))
                            .containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11);
                    }
                    return true;
                }
            )
            .assertNoErrors();
    }

    @Test
    @DisplayName("Should get the good messages count thanks to cursor param")
    void shouldGetCorrectContentTypePagination(WebClient client) {
        final TestObserver<HttpResponse<Buffer>> obs = client
            .get("/test")
            .addQueryParam(CURSOR_QUERY_PARAM, "9")
            .putHeader(HttpHeaderNames.ACCEPT.toString(), MediaType.APPLICATION_JSON)
            .rxSend()
            .test();

        awaitTerminalEvent(obs)
            .assertComplete()
            .assertValue(
                response -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    assertThat(response.getHeader(HttpHeaderNames.CONTENT_TYPE)).isEqualTo(MediaType.APPLICATION_JSON);
                    final JsonObject content = response.bodyAsJsonObject();

                    final JsonObject pagination = content.getJsonObject("pagination");
                    assertThat(pagination.getString(CURSOR_QUERY_PARAM)).isEqualTo("9");
                    // This value is due to the entrypoint "messagesLimitCount=" configuration field
                    assertThat(pagination.getString("nextCursor")).isEqualTo("14");

                    final JsonArray items = content.getJsonArray("items");
                    assertThat(items)
                        .hasSize(5)
                        .map(json -> Integer.parseInt(((JsonObject) json).getString("id")))
                        .containsExactly(10, 11, 12, 13, 14);

                    return true;
                }
            )
            .assertNoErrors();
    }

    @Test
    @DisplayName("Should get the good messages count when endpoint limits the numbers of messages")
    @DeployApi("/apis/http-get-entrypoint-limiting-mock.json")
    void shouldGetCorrectContentTypePaginationLimitingMock(WebClient client) {
        final TestObserver<HttpResponse<Buffer>> obs = client
            .get("/test-limiting-mock")
            .putHeader(HttpHeaderNames.ACCEPT.toString(), MediaType.APPLICATION_JSON)
            .rxSend()
            .test();

        awaitTerminalEvent(obs)
            .assertComplete()
            .assertValue(
                response -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    assertThat(response.getHeader(HttpHeaderNames.CONTENT_TYPE)).isEqualTo(MediaType.APPLICATION_JSON);
                    final JsonObject content = response.bodyAsJsonObject();

                    final JsonObject pagination = content.getJsonObject("pagination");
                    assertThat(pagination.getString(CURSOR_QUERY_PARAM)).isNull();
                    // This value is due to the entrypoint "messagesLimitCount" configuration field
                    assertThat(pagination.getString("nextCursor")).isEqualTo("7");

                    final JsonArray items = content.getJsonArray("items");
                    assertThat(items)
                        .hasSize(8)
                        .map(json -> Integer.parseInt(((JsonObject) json).getString("id")))
                        .containsExactly(0, 1, 2, 3, 4, 5, 6, 7);

                    return true;
                }
            )
            .assertNoErrors();
    }

    @Test
    @DisplayName("Should get the same count of messages whatever the cursor when endpoint provides messages in an unlimited way")
    @DeployApi("/apis/http-get-entrypoint-unlimited-mock.json")
    void shouldGetCorrectContentTypePaginationUnlimitedMock(WebClient client) {
        TestObserver<HttpResponse<Buffer>> obs = client
            .get("/test-unlimited-mock")
            .putHeader(HttpHeaderNames.ACCEPT.toString(), MediaType.APPLICATION_JSON)
            .rxSend()
            .test();

        awaitTerminalEvent(obs)
            .assertComplete()
            .assertValue(
                response -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    assertThat(response.getHeader(HttpHeaderNames.CONTENT_TYPE)).isEqualTo(MediaType.APPLICATION_JSON);
                    final JsonObject content = response.bodyAsJsonObject();

                    final JsonObject pagination = content.getJsonObject("pagination");
                    assertThat(pagination.getString(CURSOR_QUERY_PARAM)).isNull();
                    // This value is due to the entrypoint "messagesLimitCount" configuration field
                    assertThat(pagination.getString("nextCursor")).isEqualTo("11");

                    final JsonArray items = content.getJsonArray("items");
                    assertThat(items)
                        .hasSize(12)
                        .map(json -> Integer.parseInt(((JsonObject) json).getString("id")))
                        .containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11);

                    return true;
                }
            )
            .assertNoErrors();

        obs =
            client
                .get("/test-unlimited-mock")
                .addQueryParam(CURSOR_QUERY_PARAM, "14")
                .putHeader(HttpHeaderNames.ACCEPT.toString(), MediaType.APPLICATION_JSON)
                .rxSend()
                .test();

        awaitTerminalEvent(obs)
            .assertComplete()
            .assertValue(
                response -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    assertThat(response.getHeader(HttpHeaderNames.CONTENT_TYPE)).isEqualTo(MediaType.APPLICATION_JSON);
                    final JsonObject content = response.bodyAsJsonObject();

                    final JsonObject pagination = content.getJsonObject("pagination");
                    assertThat(pagination.getString(CURSOR_QUERY_PARAM)).isEqualTo("14");
                    // This value is due to the entrypoint "messagesLimitCount" configuration field
                    assertThat(pagination.getString("nextCursor")).isEqualTo("26");

                    final JsonArray items = content.getJsonArray("items");
                    assertThat(items)
                        .hasSize(12)
                        .map(json -> Integer.parseInt(((JsonObject) json).getString("id")))
                        .containsExactly(15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26);

                    return true;
                }
            )
            .assertNoErrors();
    }

    @Test
    @DisplayName("Should limit the messages received by the endpoint")
    void shouldUseLimit(WebClient client) {
        final TestObserver<HttpResponse<Buffer>> obs = client
            .get("/test")
            .addQueryParam(LIMIT_QUERY_PARAM, "3")
            .putHeader(HttpHeaderNames.ACCEPT.toString(), MediaType.APPLICATION_JSON)
            .rxSend()
            .test();

        awaitTerminalEvent(obs)
            .assertComplete()
            .assertValue(
                response -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    assertThat(response.getHeader(HttpHeaderNames.CONTENT_TYPE)).isEqualTo(MediaType.APPLICATION_JSON);
                    final JsonObject content = response.bodyAsJsonObject();

                    final JsonObject pagination = content.getJsonObject("pagination");
                    assertThat(pagination.getString(LIMIT_QUERY_PARAM)).isEqualTo("3");
                    // This value is due to the entrypoint "messagesLimitCount" configuration field
                    assertThat(pagination.getString("nextCursor")).isEqualTo("2");

                    final JsonArray items = content.getJsonArray("items");
                    assertThat(items)
                        .hasSize(3)
                        .map(json -> Integer.parseInt(((JsonObject) json).getString("id")))
                        .containsExactly(0, 1, 2);

                    return true;
                }
            )
            .assertNoErrors();
    }
}
