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
package io.gravitee.apim.integration.tests.messages.sharedpolicygroups;

import static org.assertj.core.api.Assertions.assertThat;

import com.graviteesource.entrypoint.http.get.HttpGetEntrypointConnectorFactory;
import com.graviteesource.entrypoint.http.post.HttpPostEntrypointConnectorFactory;
import com.graviteesource.reactor.message.MessageApiReactorFactory;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeploySharedPolicyGroups;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.fakes.MessageStorage;
import io.gravitee.apim.gateway.tests.sdk.connector.fakes.PersistentMockEndpointConnectorFactory;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.apim.gateway.tests.sdk.reactor.ReactorBuilder;
import io.gravitee.apim.plugin.reactor.ReactorPlugin;
import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactory;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.policy.transformheaders.TransformHeadersPolicy;
import io.gravitee.policy.transformheaders.configuration.TransformHeadersPolicyConfiguration;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.core.http.HttpClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SharedPolicyGroupMessageIntegrationTest {

    @Nested
    @DeploySharedPolicyGroups(
        { "/sharedpolicygroups/messages/spg-header-message-request.json", "/sharedpolicygroups/messages/spg-header-message-response.json" }
    )
    class GeneralCases extends TestPreparer {

        @Test
        @DeployApi("/apis/v4/messages/sharedpolicygroups/shared-policy-group.json")
        void should_use_shared_policy_group_on_message_request(HttpClient httpClient) {
            httpClient
                .rxRequest(HttpMethod.POST, "/test")
                .flatMap(request -> {
                    request.putHeader(HttpHeaderNames.ACCEPT.toString(), MediaType.APPLICATION_JSON);
                    return request.rxSend("a message");
                })
                .test()
                .awaitDone(30, TimeUnit.SECONDS)
                .assertValue(response -> {
                    assertThat(response.statusCode()).isEqualTo(202);
                    return true;
                })
                .assertComplete();

            messageStorage
                .subject()
                .test()
                .assertValue(message -> {
                    assertThat(message.headers().get("X-Request-Header-Dev-0")).isEqualTo("Header Dev 0");
                    return true;
                })
                .dispose();
        }

        @Test
        @DeployApi("/apis/v4/messages/sharedpolicygroups/shared-policy-group.json")
        void should_use_shared_policy_group_on_message_response(HttpClient httpClient) {
            final int messageCount = 12;

            final TestSubscriber<JsonObject> obs = httpClient
                .rxRequest(HttpMethod.GET, "/test")
                .flatMap(request -> {
                    request.putHeader(HttpHeaderNames.ACCEPT.toString(), MediaType.APPLICATION_JSON);
                    return request.rxSend();
                })
                .doOnSuccess(response -> assertThat(response.statusCode()).isEqualTo(200))
                .flatMapPublisher(response -> response.rxBody().flatMapPublisher(this::extractMessages))
                .test()
                .awaitDone(30, TimeUnit.SECONDS)
                .assertValueCount(messageCount)
                .assertComplete();

            for (int i = 0; i < messageCount; i++) {
                final int counter = i;
                obs.assertValueAt(
                    i,
                    jsonObject -> {
                        final Integer messageCounter = Integer.parseInt(jsonObject.getString("id"));
                        assertThat(messageCounter).isEqualTo(counter);
                        assertThat(jsonObject.getString("content")).matches("message");
                        assertThat(jsonObject.getJsonObject("headers").getJsonArray("X-Response-Header-Dev-0")).contains("Header Dev 0");

                        return true;
                    }
                );
            }
        }
    }

    @Nested
    @DeploySharedPolicyGroups({ "/sharedpolicygroups/messages/spg-header-message-response-conditional.json" })
    class Conditions extends TestPreparer {

        @Test
        @DeployApi("/apis/v4/messages/sharedpolicygroups/shared-policy-group-conditional.json")
        void should_not_execute_conditional_shared_policy_group(HttpClient httpClient) {
            final int messageCount = 12;

            final TestSubscriber<JsonObject> obs = httpClient
                .rxRequest(HttpMethod.GET, "/test")
                .flatMap(request -> {
                    request
                        .putHeader(HttpHeaderNames.ACCEPT.toString(), MediaType.APPLICATION_JSON)
                        .putHeader("execute-shared-policy-group", "false");
                    return request.rxSend();
                })
                .doOnSuccess(response -> assertThat(response.statusCode()).isEqualTo(200))
                .flatMapPublisher(response -> response.rxBody().flatMapPublisher(this::extractMessages))
                .test()
                .awaitDone(30, TimeUnit.SECONDS)
                .assertValueCount(messageCount)
                .assertComplete();

            for (int i = 0; i < messageCount; i++) {
                final int counter = i;
                obs.assertValueAt(
                    i,
                    jsonObject -> {
                        final Integer messageCounter = Integer.parseInt(jsonObject.getString("id"));
                        assertThat(messageCounter).isEqualTo(counter);
                        assertThat(jsonObject.getString("content")).matches("message");
                        assertThat(jsonObject.getString("headers")).doesNotContain("X-Response-Header-Dev");

                        return true;
                    }
                );
            }
        }

        @Test
        @DeployApi("/apis/v4/messages/sharedpolicygroups/shared-policy-group-conditional.json")
        void should_not_execute_conditional_policy_on_message_of_a_shared_policy_group(HttpClient httpClient) {
            final int messageCount = 12;

            final TestSubscriber<JsonObject> obs = httpClient
                .rxRequest(HttpMethod.GET, "/test")
                .flatMap(request -> {
                    request
                        .putHeader(HttpHeaderNames.ACCEPT.toString(), MediaType.APPLICATION_JSON)
                        .putHeader("execute-shared-policy-group", "yes");
                    return request.rxSend();
                })
                .doOnSuccess(response -> assertThat(response.statusCode()).isEqualTo(200))
                .flatMapPublisher(response -> response.rxBody().flatMapPublisher(this::extractMessages))
                .test()
                .awaitDone(30, TimeUnit.SECONDS)
                .assertValueCount(messageCount)
                .assertComplete();

            for (int i = 0; i < messageCount; i++) {
                final int counter = i;
                obs.assertValueAt(
                    i,
                    jsonObject -> {
                        final Integer messageCounter = Integer.parseInt(jsonObject.getString("id"));
                        assertThat(messageCounter).isEqualTo(counter);
                        assertThat(jsonObject.getString("content")).matches("message");
                        // Because of the condition, we expect only messages with even id to have the X-Response-Header-Dev-0 added
                        final JsonObject headers = jsonObject.getJsonObject("headers");
                        if (counter % 2 == 0) {
                            assertThat(headers.getJsonArray("X-Response-Header-Dev-0").getList())
                                .isEqualTo(List.of("I'm an even message!"));
                        } else {
                            assertThat(headers.getJsonArray("X-Response-Header-Dev-0")).isNull();
                        }
                        assertThat(headers.getJsonArray("X-Response-Header-Dev-1").getList()).isEqualTo(List.of("Header Dev 1"));

                        return true;
                    }
                );
            }
        }
    }

    @GatewayTest
    static class TestPreparer extends AbstractGatewayTest {

        protected MessageStorage messageStorage;

        @BeforeEach
        void setUp() {
            messageStorage = getBean(MessageStorage.class);
        }

        @AfterEach
        void tearDown() {
            messageStorage.reset();
        }

        @Override
        public void configureReactors(Set<ReactorPlugin<? extends ReactorFactory<?>>> reactors) {
            reactors.add(ReactorBuilder.build(MessageApiReactorFactory.class));
        }

        @Override
        public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
            entrypoints.putIfAbsent("http-get", EntrypointBuilder.build("http-get", HttpGetEntrypointConnectorFactory.class));
            entrypoints.putIfAbsent("http-post", EntrypointBuilder.build("http-post", HttpPostEntrypointConnectorFactory.class));
        }

        @Override
        public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
            endpoints.putIfAbsent("mock", EndpointBuilder.build("mock", PersistentMockEndpointConnectorFactory.class));
        }

        @Override
        public void configurePolicies(Map<String, PolicyPlugin> policies) {
            super.configurePolicies(policies);
            policies.put(
                "transform-headers",
                PolicyBuilder.build("transform-headers", TransformHeadersPolicy.class, TransformHeadersPolicyConfiguration.class)
            );
        }

        @NonNull
        protected Flowable<JsonObject> extractMessages(Buffer body) {
            final JsonObject jsonResponse = new JsonObject(body.toString());
            final JsonArray items = jsonResponse.getJsonArray("items");
            final List<JsonObject> messages = new ArrayList<>();

            for (int i = 0; i < items.size(); i++) {
                messages.add(items.getJsonObject(i));
            }

            return Flowable.fromIterable(messages);
        }
    }
}
