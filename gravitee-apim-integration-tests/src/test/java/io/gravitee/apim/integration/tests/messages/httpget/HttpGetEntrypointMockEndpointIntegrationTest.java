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
package io.gravitee.apim.integration.tests.messages.httpget;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.graviteesource.entrypoint.http.get.HttpGetEntrypointConnectorFactory;
import com.graviteesource.reactor.message.MessageApiReactorFactory;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.parameters.GatewayDynamicConfig;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.apim.gateway.tests.sdk.reactor.ReactorBuilder;
import io.gravitee.apim.integration.tests.fake.LatencyPolicy;
import io.gravitee.apim.plugin.reactor.ReactorPlugin;
import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.reactive.core.connection.ConnectionDrainManager;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import io.vertx.rxjava3.core.http.HttpClientResponse;
import io.vertx.rxjava3.core.http.HttpHeaders;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ParameterContext;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@GatewayTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class HttpGetEntrypointMockEndpointIntegrationTest extends AbstractGatewayTest {

    private static final XmlMapper XML_MAPPER = new XmlMapper();

    @Override
    public void configureReactors(Set<ReactorPlugin<? extends ReactorFactory<?>>> reactors) {
        reactors.add(ReactorBuilder.build(MessageApiReactorFactory.class));
    }

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("http-get", EntrypointBuilder.build("http-get", HttpGetEntrypointConnectorFactory.class));
    }

    @Override
    public void configurePolicies(Map<String, PolicyPlugin> policies) {
        super.configurePolicies(policies);
        policies.put("latency", PolicyBuilder.build("latency", LatencyPolicy.class, LatencyPolicy.LatencyConfiguration.class));
    }

    @Override
    protected void configureGateway(GatewayConfigurationBuilder gatewayConfigurationBuilder) {
        super.configureGateway(gatewayConfigurationBuilder);
        gatewayConfigurationBuilder.set("http.requestTimeout", "300");
    }

    @Override
    protected void configureHttpClient(
        HttpClientOptions options,
        GatewayDynamicConfig.Config gatewayConfig,
        ParameterContext parameterContext
    ) {
        super.configureHttpClient(options, gatewayConfig, parameterContext);

        // Force pool to 1 connection. This allows to ease the connection drain test.
        options.setMaxPoolSize(1);
    }

    @Test
    @DeployApi("/apis/v4/messages/http-get/http-get-entrypoint-mock-endpoint.json")
    void should_receive_messages_limited_by_message_count_limit(HttpClient httpClient) {
        final int messageCount = 12;

        final TestSubscriber<JsonObject> obs = createGetRequest("/test", MediaType.APPLICATION_JSON, httpClient)
            .doOnSuccess(response -> assertThat(response.statusCode()).isEqualTo(200))
            .flatMapPublisher(response -> response.rxBody().flatMapPublisher(this::extractMessages))
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValueCount(messageCount)
            .assertComplete();

        verifyMessagesAreOrdered(messageCount, obs);
    }

    @Test
    @DeployApi("/apis/v4/messages/http-get/http-get-entrypoint-mock-endpoint.json")
    void should_receive_messages_limited_by_limit_query_param(HttpClient httpClient) {
        final int messageCount = 5;

        final TestSubscriber<JsonObject> obs = createGetRequest("/test?limit=5", MediaType.APPLICATION_JSON, httpClient)
            .doOnSuccess(response -> assertThat(response.statusCode()).isEqualTo(200))
            .flatMapPublisher(response -> response.rxBody().flatMapPublisher(this::extractMessages))
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValueCount(messageCount)
            .assertComplete();

        verifyMessagesAreOrdered(messageCount, obs);
    }

    @Test
    @DeployApi("/apis/v4/messages/http-get/http-get-entrypoint-mock-endpoint-with-limit.json")
    void should_receive_messages_limited_by_what_is_produced_by_mock(HttpClient httpClient) {
        final int messageCount = 5;

        final TestSubscriber<JsonObject> obs = createGetRequest("/test", MediaType.APPLICATION_JSON, httpClient)
            .doOnSuccess(response -> assertThat(response.statusCode()).isEqualTo(200))
            .flatMapPublisher(response -> response.rxBody().flatMapPublisher(this::extractMessages))
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValueCount(messageCount)
            .assertComplete();

        verifyMessagesAreOrdered(messageCount, obs);
    }

    @Test
    @DeployApi("/apis/v4/messages/http-get/http-get-entrypoint-mock-endpoint-with-message-interval.json")
    void should_receive_no_messages_when_message_limit_duration_reached(HttpClient httpClient) {
        final int messageCount = 0;

        final TestSubscriber<JsonObject> obs = createGetRequest("/test", MediaType.APPLICATION_JSON, httpClient)
            .doOnSuccess(response -> assertThat(response.statusCode()).isEqualTo(200))
            .flatMapPublisher(response -> response.rxBody().flatMapPublisher(this::extractMessages))
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValueCount(messageCount)
            .assertComplete();

        verifyMessagesAreOrdered(messageCount, obs);
    }

    @Test
    @DeployApi("/apis/v4/messages/http-get/http-get-entrypoint-mock-endpoint-with-headers-and-metadata.json")
    void should_receive_messages_with_headers_and_metadata(HttpClient httpClient) {
        final int messageCount = 12;

        final TestSubscriber<JsonObject> obs = createGetRequest("/test", MediaType.APPLICATION_JSON, httpClient)
            .doOnSuccess(response -> assertThat(response.statusCode()).isEqualTo(200))
            .flatMapPublisher(response -> response.rxBody().flatMapPublisher(this::extractMessages))
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValueCount(messageCount)
            .assertComplete();

        verifyMessagesAreOrdered(messageCount, obs);
        verifyMessageHeadersAndMetadata(messageCount, obs);
    }

    @Test
    @DeployApi("/apis/v4/messages/http-get/http-get-entrypoint-mock-endpoint.json")
    void should_receive_messages_xml(HttpClient httpClient) {
        final int messageCount = 12;

        final TestSubscriber<JsonObject> obs = createGetRequest("/test", MediaType.APPLICATION_XML, httpClient)
            .doOnSuccess(response -> assertThat(response.statusCode()).isEqualTo(200))
            .flatMapPublisher(response -> response.rxBody().flatMapPublisher(this::extractXmlMessages))
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValueCount(messageCount)
            .assertComplete();

        verifyMessagesAreOrdered(messageCount, obs);
    }

    @Test
    @DeployApi("/apis/v4/messages/http-get/http-get-entrypoint-mock-endpoint.json")
    void should_receive_messages_text_plain(HttpClient httpClient) {
        final int messageCount = 12;

        final TestSubscriber<JsonObject> obs = createGetRequest("/test", MediaType.TEXT_PLAIN, httpClient)
            .doOnSuccess(response -> assertThat(response.statusCode()).isEqualTo(200))
            .flatMapPublisher(response -> response.rxBody().flatMapPublisher(this::extractPlainTextMessages))
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValueCount(messageCount)
            .assertComplete();

        verifyMessagesAreOrdered(messageCount, obs);
    }

    @Test
    @DeployApi("/apis/v4/messages/http-get/http-get-entrypoint-mock-endpoint-timeout.json")
    void should_return_504_when_gateway_timeout_is_reached(HttpClient httpClient) {
        final int messageCount = 0;

        final TestObserver<JsonObject> obs = createGetRequest("/test", MediaType.APPLICATION_JSON, httpClient)
            .doOnSuccess(response -> assertThat(response.statusCode()).isEqualTo(504))
            .flatMap(response -> response.rxBody().map(body -> new JsonObject(body.toString())))
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValue(body -> {
                assertThat(body.getString("message")).isEqualTo("Request timeout");
                assertThat(body.getInteger("http_status_code")).isEqualTo(504);
                return true;
            })
            .assertComplete();
    }

    @Test
    @DeployApi("/apis/v4/messages/http-get/http-get-entrypoint-mock-endpoint.json")
    void should_return_connection_close_header_when_connection_is_drained(HttpClient client) {
        final ConnectionDrainManager connectionDrainManager = applicationContext.getBean(ConnectionDrainManager.class);

        client
            .rxRequest(HttpMethod.GET, "/test")
            .concatMap(HttpClientRequest::rxSend)
            .concatMap(response -> response.end().andThen(Single.just(response.headers())))
            // Request connection drain after first request. The second request should be asked to close connection.
            .doOnSuccess(headers -> connectionDrainManager.requestDrain())
            .repeat(2)
            // Keep the last response only.
            .lastElement()
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertComplete()
            // Last response should contain a Connection: close header.
            .assertValue(headers -> headers.get(HttpHeaders.CONNECTION).equals("close"));
    }

    @NonNull
    private Flowable<JsonObject> extractMessages(Buffer body) {
        final JsonObject jsonResponse = new JsonObject(body.toString());
        final JsonArray items = jsonResponse.getJsonArray("items");
        final List<JsonObject> messages = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            messages.add(items.getJsonObject(i));
        }

        return Flowable.fromIterable(messages);
    }

    @NonNull
    private Flowable<JsonObject> extractPlainTextMessages(Buffer body) {
        final List<JsonObject> messages = new ArrayList<>();

        final String[] lines = body.toString().split("\n");

        JsonObject jsonObject = new JsonObject();

        for (String line : lines) {
            if (line.equals("item")) {
                jsonObject = new JsonObject();
                messages.add(jsonObject);
            } else if (line.startsWith("id:")) {
                jsonObject.put("id", Integer.parseInt(line.replace("id: ", "")));
            } else if (line.startsWith("content:")) {
                jsonObject.put("content", line.replace("content: ", ""));
            }
        }

        return Flowable.fromIterable(messages);
    }

    @NonNull
    private Flowable<JsonObject> extractXmlMessages(Buffer body) {
        final List<JsonObject> messages = new ArrayList<>();

        try {
            final JsonNode jsonNode = XML_MAPPER.readTree(body.toString());
            final JsonNode items = jsonNode.get("items").get("item");

            for (int i = 0; i < items.size(); i++) {
                messages.add(new JsonObject(items.get(i).toString()));
            }
            return Flowable.fromIterable(messages);
        } catch (JsonProcessingException e) {
            return Flowable.error(e);
        }
    }

    @NonNull
    private Single<HttpClientResponse> createGetRequest(String path, String accept, HttpClient httpClient) {
        return httpClient
            .rxRequest(HttpMethod.GET, path)
            .flatMap(request -> {
                request.putHeader(HttpHeaderNames.ACCEPT.toString(), accept);
                return request.rxSend();
            });
    }

    private void verifyMessagesAreOrdered(int messageCount, TestSubscriber<JsonObject> obs) {
        for (int i = 0; i < messageCount; i++) {
            final int counter = i;
            obs.assertValueAt(
                i,
                jsonObject -> {
                    final Integer messageCounter = Integer.parseInt(jsonObject.getString("id"));
                    assertThat(messageCounter).isEqualTo(counter);
                    assertThat(jsonObject.getString("content")).matches("message");

                    return true;
                }
            );
        }
    }

    private void verifyMessageHeadersAndMetadata(int messageCount, TestSubscriber<JsonObject> obs) {
        for (int i = 0; i < messageCount; i++) {
            obs.assertValueAt(
                i,
                jsonObject -> {
                    final JsonObject headers = jsonObject.getJsonObject("headers");
                    assertThat(headers.getJsonArray("header1").getList()).isEqualTo(List.of("headerValue1"));

                    final JsonObject metadata = jsonObject.getJsonObject("metadata");
                    assertThat(metadata.getString("metadata1")).isEqualTo("metadataValue1");

                    return true;
                }
            );
        }
    }
}
