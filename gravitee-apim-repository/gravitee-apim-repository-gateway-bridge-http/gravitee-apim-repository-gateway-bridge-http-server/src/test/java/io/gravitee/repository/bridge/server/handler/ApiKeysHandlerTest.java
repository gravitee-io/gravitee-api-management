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
package io.gravitee.repository.bridge.server.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.ApiKeyCriteria;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import io.gravitee.repository.management.model.ApiKey;
import io.gravitee.repository.management.model.Subscription;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(VertxExtension.class)
public class ApiKeysHandlerTest {

    private final WorkerExecutor executor = Vertx.vertx().createSharedWorkerExecutor("apiKeysHandlerTestWorker");

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @InjectMocks
    private final ApiKeysHandler apiKeysHandler = new ApiKeysHandler(executor);

    private WebClient client;

    @BeforeEach
    public void beforeEach(Vertx vertx, VertxTestContext testContext) throws Exception {
        MockitoAnnotations.openMocks(this);

        Router router = Router.router(vertx);

        router.route().handler(BodyHandler.create());
        router.post("/_search").handler(apiKeysHandler::search);
        router.post("/_findByCriteria").handler(apiKeysHandler::findByCriteria);

        int port = getRandomPort();

        client = WebClient.create(vertx, new WebClientOptions().setDefaultHost("localhost").setDefaultPort(port));

        vertx.deployVerticle(
            new AbstractVerticle() {
                @Override
                public void start() {
                    vertx.createHttpServer().requestHandler(router).listen(port);
                }
            },
            testContext.succeedingThenComplete()
        );
    }

    @AfterEach
    public void afterEach(Vertx vertx, VertxTestContext testContext) {
        vertx.close(testContext.succeedingThenComplete());
        executor.close();
    }

    @Nested
    class SearchTest {

        @Test
        @Deprecated
        void should_return_apiKey_with_api_plan_subscription(VertxTestContext testContext) throws TechnicalException {
            Subscription subscription = new Subscription();
            subscription.setId("subscription-id");
            subscription.setApi("subscription-api-id");
            subscription.setPlan("subscription-plan-id");

            ApiKey apiKey = new ApiKey();
            apiKey.setSubscriptions(List.of("subscription-id"));

            when(apiKeyRepository.findByCriteria(ApiKeyCriteria.builder().build(), null)).thenReturn(List.of(apiKey));
            when(subscriptionRepository.findByIdIn(argThat(ids -> apiKey.getSubscriptions().containsAll(ids))))
                .thenReturn(List.of(subscription));

            client
                .post("/_search")
                .expect(ResponsePredicate.SC_OK)
                .expect(ResponsePredicate.JSON)
                .as(BodyCodec.jsonArray())
                .sendJsonObject(new JsonObject())
                .onComplete(
                    testContext.succeeding(response ->
                        testContext.verify(() -> {
                            JsonArray responseBody = response.body();
                            assertThat(responseBody.size()).isEqualTo(1);
                            JsonObject responseApiKey = responseBody.getJsonObject(0);
                            assertThat(responseApiKey.getString("subscription")).isEqualTo("subscription-id");
                            assertThat(responseApiKey.getString("api")).isEqualTo("subscription-api-id");
                            assertThat(responseApiKey.getString("plan")).isEqualTo("subscription-plan-id");
                            testContext.completeNow();
                        })
                    )
                )
                .onFailure(testContext::failNow);
        }
    }

    @Nested
    class CriteriaTest {

        @Test
        void should_return_apiKey_with_subscriptions_without_criteria(VertxTestContext testContext) throws TechnicalException {
            ApiKey apiKey = new ApiKey();
            apiKey.setSubscriptions(List.of("subscription-id1", "subscription-id2"));

            when(apiKeyRepository.findByCriteria(ApiKeyCriteria.builder().build(), null)).thenReturn(List.of(apiKey));

            client
                .post("/_findByCriteria")
                .expect(ResponsePredicate.SC_OK)
                .expect(ResponsePredicate.JSON)
                .as(BodyCodec.jsonArray())
                .send()
                .onComplete(
                    testContext.succeeding(response ->
                        testContext.verify(() -> {
                            JsonArray responseBody = response.body();
                            assertThat(responseBody.size()).isEqualTo(1);
                            JsonObject responseApiKey = responseBody.getJsonObject(0);
                            assertThat(responseApiKey.getString("subscriptions")).isEqualTo("[subscription-id1, subscription-id2]");
                            assertThat(responseApiKey.containsKey("api")).isFalse();
                            assertThat(responseApiKey.containsKey("plan")).isFalse();
                            testContext.completeNow();
                        })
                    )
                )
                .onFailure(testContext::failNow);
        }

        @Test
        void should_return_apiKey_with_subscriptions_with_criteria(VertxTestContext testContext) throws TechnicalException {
            ApiKey apiKey = new ApiKey();
            apiKey.setSubscriptions(List.of("subscription-id1", "subscription-id2"));

            ApiKeyCriteria apiKeyCriteria = ApiKeyCriteria
                .builder()
                .subscriptions(List.of("subscription-id1", "subscription-id2"))
                .to(123)
                .from(123)
                .expireAfter(123)
                .expireBefore(123)
                .includeWithoutExpiration(true)
                .build();
            when(apiKeyRepository.findByCriteria(apiKeyCriteria, null)).thenReturn(List.of(apiKey));

            client
                .post("/_findByCriteria")
                .expect(ResponsePredicate.SC_OK)
                .expect(ResponsePredicate.JSON)
                .as(BodyCodec.jsonArray())
                .sendJson(apiKeyCriteria)
                .onComplete(
                    testContext.succeeding(response ->
                        testContext.verify(() -> {
                            JsonArray responseBody = response.body();
                            assertThat(responseBody.size()).isEqualTo(1);
                            JsonObject responseApiKey = responseBody.getJsonObject(0);
                            assertThat(responseApiKey.getString("subscriptions")).isEqualTo("[subscription-id1, subscription-id2]");
                            assertThat(responseApiKey.containsKey("api")).isFalse();
                            assertThat(responseApiKey.containsKey("plan")).isFalse();
                            testContext.completeNow();
                        })
                    )
                )
                .onFailure(testContext::failNow);
        }

        @Test
        void should_return_apiKey_without_criteria_but_with_sortable(VertxTestContext testContext) throws TechnicalException {
            ApiKey apiKey = new ApiKey();
            apiKey.setSubscriptions(List.of("subscription-id1", "subscription-id2"));

            when(
                apiKeyRepository.findByCriteria(
                    ApiKeyCriteria.builder().build(),
                    new SortableBuilder().order(Order.ASC).field("field").build()
                )
            )
                .thenReturn(List.of(apiKey));

            client
                .post("/_findByCriteria")
                .addQueryParam("order", "ASC")
                .addQueryParam("field", "field")
                .expect(ResponsePredicate.SC_OK)
                .expect(ResponsePredicate.JSON)
                .as(BodyCodec.jsonArray())
                .sendJsonObject(new JsonObject())
                .onComplete(
                    testContext.succeeding(response ->
                        testContext.verify(() -> {
                            JsonArray responseBody = response.body();
                            assertThat(responseBody.size()).isEqualTo(1);
                            JsonObject responseApiKey = responseBody.getJsonObject(0);
                            assertThat(responseApiKey.getString("subscriptions")).isEqualTo("[subscription-id1, subscription-id2]");
                            assertThat(responseApiKey.containsKey("api")).isFalse();
                            assertThat(responseApiKey.containsKey("plan")).isFalse();
                            testContext.completeNow();
                        })
                    )
                )
                .onFailure(testContext::failNow);
        }
    }

    private int getRandomPort() throws IOException {
        ServerSocket socket = new ServerSocket(0);
        int port = socket.getLocalPort();
        socket.close();
        return port;
    }
}
