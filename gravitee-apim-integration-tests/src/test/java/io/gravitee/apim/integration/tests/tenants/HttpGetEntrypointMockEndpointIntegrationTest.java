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
package io.gravitee.apim.integration.tests.tenants;

import static org.assertj.core.api.Assertions.assertThat;

import com.graviteesource.entrypoint.http.get.HttpGetEntrypointConnectorFactory;
import com.graviteesource.reactor.message.MessageApiReactorFactory;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.reactor.ReactorBuilder;
import io.gravitee.apim.plugin.reactor.ReactorPlugin;
import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
@GatewayTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class HttpGetEntrypointMockEndpointIntegrationTest extends AbstractGatewayTest {

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("http-get", EntrypointBuilder.build("http-get", HttpGetEntrypointConnectorFactory.class));
    }

    @Override
    public void configureGateway(GatewayConfigurationBuilder configurationBuilder) {
        super.configureGateway(configurationBuilder);
        configurationBuilder.set("tenant", "tenant-1");
    }

    @Override
    public void configureReactors(Set<ReactorPlugin<? extends ReactorFactory<?>>> reactors) {
        reactors.add(ReactorBuilder.build(MessageApiReactorFactory.class));
    }

    @Test
    @DeployApi("/apis/v4/messages/http-get/http-get-entrypoint-mock-endpoint-with-tenants.json")
    void should_receive_all_messages_from_endpoint_with_tenant_1_only(HttpClient httpClient) {
        for (int i = 0; i < 2; i++) {
            createGetRequest("/test", MediaType.APPLICATION_JSON, httpClient)
                .doOnSuccess(response -> assertThat(response.statusCode()).isEqualTo(200))
                .flatMapPublisher(response ->
                    response.rxBody().flatMapPublisher(HttpGetEntrypointMockEndpointIntegrationTest.this::extractMessages).take(1)
                )
                .test()
                .awaitDone(30, TimeUnit.SECONDS)
                .assertValue(jsonObject -> {
                    assertThat(jsonObject.getString("content")).isEqualTo("message from endpoint tenant-1");
                    return true;
                })
                .assertComplete();
        }
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
    private Single<HttpClientResponse> createGetRequest(String path, String accept, HttpClient httpClient) {
        return httpClient
            .rxRequest(HttpMethod.GET, path)
            .flatMap(request -> {
                request.putHeader(HttpHeaderNames.ACCEPT.toString(), accept);
                return request.rxSend();
            });
    }
}
