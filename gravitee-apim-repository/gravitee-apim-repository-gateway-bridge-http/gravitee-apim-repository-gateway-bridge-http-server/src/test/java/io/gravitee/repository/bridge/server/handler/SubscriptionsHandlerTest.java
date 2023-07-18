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
import static org.mockito.Mockito.when;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
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
import java.util.Set;
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
import org.springframework.test.util.TestSocketUtils;

/**
 * @author GraviteeSource Team
 */
@ExtendWith(VertxExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class SubscriptionsHandlerTest {

    private final WorkerExecutor executor = Vertx.vertx().createSharedWorkerExecutor("subscriptionsHandlerTestWorker");

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private final SubscriptionsHandler subscriptionsHandler = new SubscriptionsHandler(executor);

    private WebClient client;

    @BeforeEach
    public void beforeEach(Vertx vertx, VertxTestContext context) throws Exception {
        MockitoAnnotations.openMocks(this);

        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.post("/_findByIds").handler(subscriptionsHandler::findByIds);
        router.post("/_search").handler(subscriptionsHandler::search);
        router.post("/_searchPageable").handler(subscriptionsHandler::searchPageable);

        int port = getAvailablePort();

        client = WebClient.create(vertx, new WebClientOptions().setDefaultHost("localhost").setDefaultPort(port));

        vertx.deployVerticle(
            new AbstractVerticle() {
                @Override
                public void start() {
                    vertx.createHttpServer().requestHandler(router).listen(port);
                }
            },
            context.succeedingThenComplete()
        );
    }

    @AfterEach
    public void afterEach(Vertx vertx, VertxTestContext testContext) {
        vertx.close(testContext.succeedingThenComplete());
        executor.close();
    }

    @Nested
    class ByIdsTest {

        // flaky
        @Test
        void should_return_subscription_list(VertxTestContext testContext) throws TechnicalException {
            when(subscriptionRepository.findByIdIn(List.of("id1", "idX", "id4")))
                .thenReturn(List.of(buildSubscription("sub-id1"), buildSubscription("sub-idX"), buildSubscription("sub-id4")));

            client
                .post("/_findByIds")
                .expect(ResponsePredicate.SC_OK)
                .expect(ResponsePredicate.JSON)
                .as(BodyCodec.jsonArray())
                .sendJson(List.of("id1", "idX", "id4"))
                .onComplete(
                    testContext.succeeding(response ->
                        testContext.verify(() -> {
                            JsonArray responseBody = response.body();
                            assertThat(responseBody.size()).isEqualTo(3);
                            JsonObject responseApiKey = responseBody.getJsonObject(0);
                            assertThat(responseBody.getJsonObject(0).getString("id")).isEqualTo("sub-id1");
                            assertThat(responseBody.getJsonObject(1).getString("id")).isEqualTo("sub-idX");
                            assertThat(responseBody.getJsonObject(2).getString("id")).isEqualTo("sub-id4");
                            testContext.completeNow();
                        })
                    )
                )
                .onFailure(testContext::failNow);
        }

        // flaky
        @Test
        void should_return_a_http_500_server_error_with_empty_id_list(VertxTestContext testContext) {
            client
                .post("/_findByIds")
                .expect(ResponsePredicate.SC_INTERNAL_SERVER_ERROR)
                .sendJson(List.of())
                .onSuccess(response -> testContext.completeNow())
                .onFailure(testContext::failNow);
        }

        @Test
        void should_return_a_http_500_server_error_without_id_list(VertxTestContext testContext) {
            client
                .post("/_findByIds")
                .expect(ResponsePredicate.SC_INTERNAL_SERVER_ERROR)
                .send()
                .onSuccess(response -> testContext.completeNow())
                .onFailure(testContext::failNow);
        }
    }

    @Nested
    class SearchTest {

        @Test
        void should_return_subscriptions_without_criteria(VertxTestContext testContext) throws TechnicalException {
            when(subscriptionRepository.search(SubscriptionCriteria.builder().build(), null))
                .thenReturn(List.of(buildSubscription("sub-id1"), buildSubscription("sub-idX"), buildSubscription("sub-id4")));

            client
                .post("/_search")
                .expect(ResponsePredicate.SC_OK)
                .expect(ResponsePredicate.JSON)
                .as(BodyCodec.jsonArray())
                .send()
                .onComplete(
                    testContext.succeeding(response ->
                        testContext.verify(() -> {
                            JsonArray responseBody = response.body();
                            assertThat(responseBody.size()).isEqualTo(3);
                            assertThat(responseBody.getJsonObject(0).getString("id")).isEqualTo("sub-id1");
                            assertThat(responseBody.getJsonObject(1).getString("id")).isEqualTo("sub-idX");
                            assertThat(responseBody.getJsonObject(2).getString("id")).isEqualTo("sub-id4");
                            testContext.completeNow();
                        })
                    )
                )
                .onFailure(testContext::failNow);
        }

        @Test
        void should_return_subscriptions_with_criteria(VertxTestContext testContext) throws TechnicalException {
            SubscriptionCriteria criteria = SubscriptionCriteria
                .builder()
                .ids(Set.of("sub-id1", "sub-idX", "sub-id4"))
                .to(123)
                .from(123)
                .includeWithoutEnd(true)
                .endingAtAfter(123)
                .endingAtBefore(123)
                .statuses(Set.of("status"))
                .apis(Set.of("api"))
                .applications(Set.of("application"))
                .plans(Set.of("plan"))
                .clientId("clientId")
                .planSecurityTypes(Set.of("securityType"))
                .build();
            when(subscriptionRepository.search(criteria, null))
                .thenReturn(List.of(buildSubscription("sub-id1"), buildSubscription("sub-idX"), buildSubscription("sub-id4")));

            client
                .post("/_search")
                .expect(ResponsePredicate.SC_OK)
                .expect(ResponsePredicate.JSON)
                .as(BodyCodec.jsonArray())
                .sendJson(criteria)
                .onComplete(
                    testContext.succeeding(response ->
                        testContext.verify(() -> {
                            JsonArray responseBody = response.body();
                            assertThat(responseBody.size()).isEqualTo(3);
                            assertThat(responseBody.getJsonObject(0).getString("id")).isEqualTo("sub-id1");
                            assertThat(responseBody.getJsonObject(1).getString("id")).isEqualTo("sub-idX");
                            assertThat(responseBody.getJsonObject(2).getString("id")).isEqualTo("sub-id4");
                            testContext.completeNow();
                        })
                    )
                )
                .onFailure(testContext::failNow);
        }

        // flaky
        @Test
        void should_respond_subscriptions_without_criteria_but_with_sortable(VertxTestContext testContext) throws TechnicalException {
            when(
                subscriptionRepository.search(
                    SubscriptionCriteria.builder().build(),
                    new SortableBuilder().order(Order.ASC).field("field").build()
                )
            )
                .thenReturn(List.of(buildSubscription("sub-id1"), buildSubscription("sub-idX"), buildSubscription("sub-id4")));

            client
                .post("/_search")
                .addQueryParam("order", "ASC")
                .addQueryParam("field", "field")
                .expect(ResponsePredicate.SC_OK)
                .expect(ResponsePredicate.JSON)
                .as(BodyCodec.jsonArray())
                .send()
                .onComplete(
                    testContext.succeeding(response ->
                        testContext.verify(() -> {
                            JsonArray responseBody = response.body();
                            assertThat(responseBody.size()).isEqualTo(3);
                            assertThat(responseBody.getJsonObject(0).getString("id")).isEqualTo("sub-id1");
                            assertThat(responseBody.getJsonObject(1).getString("id")).isEqualTo("sub-idX");
                            assertThat(responseBody.getJsonObject(2).getString("id")).isEqualTo("sub-id4");
                            testContext.completeNow();
                        })
                    )
                )
                .onFailure(testContext::failNow);
        }
    }

    @Nested
    class SearchPageableTest {

        @Test
        void should_return_page_of_subscriptions_without_pageable(VertxTestContext testContext) throws TechnicalException {
            when(subscriptionRepository.search(SubscriptionCriteria.builder().build(), null, null))
                .thenReturn(
                    new Page<>(List.of(buildSubscription("sub-id1"), buildSubscription("sub-idX"), buildSubscription("sub-id4")), 1, 3, 1)
                );

            client
                .post("/_searchPageable")
                .expect(ResponsePredicate.SC_OK)
                .expect(ResponsePredicate.JSON)
                .as(BodyCodec.jsonObject())
                .send()
                .onComplete(
                    testContext.succeeding(response ->
                        testContext.verify(() -> {
                            JsonObject responseBody = response.body();
                            JsonArray content = responseBody.getJsonArray("content");
                            assertThat(content.size()).isEqualTo(3);
                            assertThat(content.getJsonObject(0).getString("id")).isEqualTo("sub-id1");
                            assertThat(content.getJsonObject(1).getString("id")).isEqualTo("sub-idX");
                            assertThat(content.getJsonObject(2).getString("id")).isEqualTo("sub-id4");
                            testContext.completeNow();
                        })
                    )
                )
                .onFailure(testContext::failNow);
        }

        @Test
        void should_return_subscriptions_with_pageable_and_criteria(VertxTestContext testContext) throws TechnicalException {
            SubscriptionCriteria criteria = SubscriptionCriteria
                .builder()
                .ids(Set.of("sub-id1", "sub-idX", "sub-id4"))
                .to(123)
                .from(123)
                .includeWithoutEnd(true)
                .endingAtAfter(123)
                .endingAtBefore(123)
                .statuses(Set.of("status"))
                .apis(Set.of("api"))
                .applications(Set.of("application"))
                .plans(Set.of("plan"))
                .clientId("clientId")
                .planSecurityTypes(Set.of("securityType"))
                .build();
            when(subscriptionRepository.search(criteria, null, new PageableBuilder().pageSize(10).pageNumber(1).build()))
                .thenReturn(
                    new Page<>(List.of(buildSubscription("sub-id1"), buildSubscription("sub-idX"), buildSubscription("sub-id4")), 1, 3, 1)
                );

            client
                .post("/_searchPageable")
                .addQueryParam("page", "1")
                .addQueryParam("size", "10")
                .expect(ResponsePredicate.SC_OK)
                .expect(ResponsePredicate.JSON)
                .as(BodyCodec.jsonObject())
                .sendJson(criteria)
                .onComplete(
                    testContext.succeeding(response ->
                        testContext.verify(() -> {
                            JsonObject responseBody = response.body();
                            JsonArray content = responseBody.getJsonArray("content");
                            assertThat(content.size()).isEqualTo(3);
                            assertThat(content.getJsonObject(0).getString("id")).isEqualTo("sub-id1");
                            assertThat(content.getJsonObject(1).getString("id")).isEqualTo("sub-idX");
                            assertThat(content.getJsonObject(2).getString("id")).isEqualTo("sub-id4");
                            testContext.completeNow();
                        })
                    )
                )
                .onFailure(testContext::failNow);
        }

        // Flaky
        @Test
        void should_respond_subscriptions_without_pageable_or_criteria_but_with_sortable(VertxTestContext testContext)
            throws TechnicalException {
            when(
                subscriptionRepository.search(
                    SubscriptionCriteria.builder().build(),
                    new SortableBuilder().order(Order.ASC).field("field").build(),
                    null
                )
            )
                .thenReturn(
                    new Page<>(List.of(buildSubscription("sub-id1"), buildSubscription("sub-idX"), buildSubscription("sub-id4")), 1, 3, 1)
                );

            client
                .post("/_searchPageable")
                .addQueryParam("order", "ASC")
                .addQueryParam("field", "field")
                .expect(ResponsePredicate.SC_OK)
                .expect(ResponsePredicate.JSON)
                .as(BodyCodec.jsonObject())
                .send()
                .onComplete(
                    testContext.succeeding(response ->
                        testContext.verify(() -> {
                            JsonObject responseBody = response.body();
                            JsonArray content = responseBody.getJsonArray("content");
                            assertThat(content.size()).isEqualTo(3);
                            assertThat(content.getJsonObject(0).getString("id")).isEqualTo("sub-id1");
                            assertThat(content.getJsonObject(1).getString("id")).isEqualTo("sub-idX");
                            assertThat(content.getJsonObject(2).getString("id")).isEqualTo("sub-id4");
                            testContext.completeNow();
                        })
                    )
                )
                .onFailure(testContext::failNow);
        }
    }

    private static int getAvailablePort() {
        return TestSocketUtils.findAvailableTcpPort();
    }

    private Subscription buildSubscription(String id) {
        Subscription subscription = new Subscription();
        subscription.setId(id);
        return subscription;
    }
}
