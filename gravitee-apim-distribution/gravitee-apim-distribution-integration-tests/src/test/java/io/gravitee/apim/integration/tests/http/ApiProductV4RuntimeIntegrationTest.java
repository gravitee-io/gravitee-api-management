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
package io.gravitee.apim.integration.tests.http;

import static io.gravitee.apim.integration.tests.plan.PlanHelper.configurePlans;
import static io.vertx.core.http.HttpMethod.GET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.execution.FlowExecution;
import io.gravitee.definition.model.v4.flow.execution.FlowMode;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.gateway.api.service.ApiKey;
import io.gravitee.gateway.api.service.ApiKeyService;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.api.service.SubscriptionService;
import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.policy.assignattributes.AssignAttributesPolicy;
import io.gravitee.policy.assignattributes.configuration.AssignAttributesPolicyConfiguration;
import io.gravitee.policy.keyless.KeylessPolicy;
import io.gravitee.policy.transformheaders.TransformHeadersPolicy;
import io.gravitee.policy.transformheaders.configuration.TransformHeadersPolicyConfiguration;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientResponse;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Product plan flows run from the product's own runtime, which a product update swaps without a gap.
 *
 * <p>Each product plan flow adds, in the request, a header naming the product version, and in the response a header
 * with the same literal plus one copying the request header: a request whose two phases ran on two runtimes would
 * answer with two different versions.</p>
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiProductV4RuntimeIntegrationTest {

    private static final String ENV_ID = "DEFAULT";
    private static final String PRODUCT_ID = "runtime-product";
    private static final String PRODUCT_PLAN_ID = "runtime-product-plan";
    private static final String API_1_ID = "my-api-v4-1";
    private static final String API_1_PATH = "/test-1";
    private static final String KEY = "runtime-product-key";
    private static final String REQUEST_VERSION_HEADER = "X-Product-Request-Version";
    private static final String RESPONSE_VERSION_HEADER = "X-Product-Response-Version";
    private static final String ECHOED_VERSION_HEADER = "X-Product-Echoed-Version";
    private static final String FIRST_FLOW_HEADER = "X-Product-First-Flow";
    private static final String SECOND_FLOW_HEADER = "X-Product-Second-Flow";

    abstract static class RuntimeTestPreparer extends ApiProductV4IntegrationTest.TestPreparer {

        @Override
        public void configurePolicies(Map<String, PolicyPlugin> policies) {
            super.configurePolicies(policies);
            policies.put(
                "transform-headers",
                PolicyBuilder.build("transform-headers", TransformHeadersPolicy.class, TransformHeadersPolicyConfiguration.class)
            );
        }

        void awaitServedBy(List<ProductResponse> responses, String version) {
            await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> responses.stream().anyMatch(response -> version.equals(response.responseVersion())));
        }

        void deployProduct(String productId, String planId, String flowVersion) {
            ReactableApiProduct product = product(productId, Set.of(API_1_ID));
            Plan plan = productPlan(planId, "api-key", PlanStatus.PUBLISHED);
            plan.setFlows(List.of(versionFlow(flowVersion)));
            product.setPlans(List.of(plan));
            deployApiProduct(product);
        }

        void deployProductVersion(int version) {
            ReactableApiProduct product = product(PRODUCT_ID, Set.of(API_1_ID));
            product.setVersion(String.valueOf(version));
            Plan plan = productPlan(PRODUCT_PLAN_ID, "api-key", PlanStatus.PUBLISHED);
            plan.setFlows(List.of(versionFlow("v" + version)));
            product.setPlans(List.of(plan));
            deployApiProduct(product);
        }

        void allowProductKey() {
            ApiKey apiKey = new ApiKey();
            apiKey.setApi(API_1_ID);
            apiKey.setApplication("application-id");
            apiKey.setSubscription("subscription-" + KEY);
            apiKey.setPlan(PRODUCT_PLAN_ID);
            apiKey.setKey(KEY);
            apiKey.setActive(true);
            apiKey.setExpireAt(new Date(System.currentTimeMillis() + 600_000));

            Subscription subscription = new Subscription();
            subscription.setId("subscription-" + KEY);
            subscription.setApplication("application-id");
            subscription.setApiProductId(PRODUCT_ID);
            subscription.setPlan(PRODUCT_PLAN_ID);
            subscription.setStatus("ACCEPTED");

            when(getBean(ApiKeyService.class).getByApiAndKey(eq(API_1_ID), eq(KEY))).thenReturn(Optional.of(apiKey));
            when(getBean(SubscriptionService.class).getByApiAndSecurityToken(eq(PRODUCT_ID), any(), eq(PRODUCT_PLAN_ID))).thenReturn(
                Optional.of(subscription)
            );
        }

        void allowProductKeyOnPlan(String key, String planId) {
            ApiKey apiKey = new ApiKey();
            apiKey.setApi(PRODUCT_ID);
            apiKey.setApplication("application-id");
            apiKey.setSubscription("subscription-" + key);
            apiKey.setPlan(planId);
            apiKey.setKey(key);
            apiKey.setActive(true);
            apiKey.setExpireAt(new Date(System.currentTimeMillis() + 600_000));

            Subscription subscription = new Subscription();
            subscription.setId("subscription-" + key);
            subscription.setApplication("application-id");
            subscription.setApiProductId(PRODUCT_ID);
            subscription.setPlan(planId);
            subscription.setStatus("ACCEPTED");

            when(getBean(ApiKeyService.class).getByApiAndKey(eq(API_1_ID), eq(key))).thenReturn(Optional.of(apiKey));
            when(
                getBean(SubscriptionService.class).getByApiAndSecurityToken(
                    eq(PRODUCT_ID),
                    argThat(token -> token != null && key.equals(token.getTokenValue())),
                    eq(planId)
                )
            ).thenReturn(Optional.of(subscription));
        }

        void deployProductVersionWithPlans(int version, List<String> planIds) {
            ReactableApiProduct product = product(PRODUCT_ID, Set.of(API_1_ID));
            product.setVersion(String.valueOf(version));
            product.setPlans(
                planIds
                    .stream()
                    .map(planId -> {
                        Plan plan = productPlan(planId, "api-key", PlanStatus.PUBLISHED);
                        plan.setFlows(List.of(versionFlow("v" + version)));
                        return plan;
                    })
                    .toList()
            );
            deployApiProduct(product);
        }

        ProductResponse call(HttpClient client) {
            return call(client, KEY);
        }

        ProductResponse call(HttpClient client, String apiKey) {
            return client
                .rxRequest(GET, API_1_PATH)
                .flatMap(request -> (apiKey == null ? request : request.putHeader("X-Gravitee-Api-Key", apiKey)).rxSend())
                .flatMap(response ->
                    response
                        .rxBody()
                        .map(body ->
                            new ProductResponse(
                                response.statusCode(),
                                response.getHeader(RESPONSE_VERSION_HEADER),
                                response.getHeader(ECHOED_VERSION_HEADER)
                            )
                        )
                )
                .blockingGet();
        }

        void deployProductWithTwoMatchingFlows() {
            ReactableApiProduct product = product(PRODUCT_ID, Set.of(API_1_ID));
            Plan plan = productPlan(PRODUCT_PLAN_ID, "api-key", PlanStatus.PUBLISHED);
            plan.setFlows(List.of(responseHeaderFlow("first", FIRST_FLOW_HEADER), responseHeaderFlow("second", SECOND_FLOW_HEADER)));
            product.setPlans(List.of(plan));
            deployApiProduct(product);
        }

        private static Flow responseHeaderFlow(String name, String header) {
            HttpSelector selector = new HttpSelector();
            selector.setPath("/");
            return Flow.builder()
                .name(name)
                .enabled(true)
                .selectors(List.of(selector))
                .response(List.of(addHeaderStep(header, "ran")))
                .build();
        }

        private static Flow versionFlow(String version) {
            HttpSelector selector = new HttpSelector();
            selector.setPath("/");
            return Flow.builder()
                .name("product plan flow " + version)
                .enabled(true)
                .selectors(List.of(selector))
                .request(List.of(addHeaderStep(REQUEST_VERSION_HEADER, version)))
                .response(
                    List.of(
                        addHeaderStep(RESPONSE_VERSION_HEADER, version),
                        addHeaderStep(ECHOED_VERSION_HEADER, "{#request.headers['" + REQUEST_VERSION_HEADER + "'][0]}")
                    )
                )
                .build();
        }

        private static Step addHeaderStep(String name, String value) {
            return Step.builder()
                .name("add " + name)
                .enabled(true)
                .policy("transform-headers")
                .configuration("{\"addHeaders\":[{\"name\":\"" + name + "\",\"value\":\"" + value + "\"}]}")
                .build();
        }
    }

    record ProductResponse(int status, String responseVersion, String echoedVersion) {}

    @Nested
    @GatewayTest
    @DeployApi({ "/apis/v4/http/api-product/api-1.json" })
    class ProductUpdates extends RuntimeTestPreparer {

        @Test
        void should_run_the_product_plan_flow_of_the_deployed_version(HttpClient client) {
            allowProductKey();
            deployProductVersion(1);

            ProductResponse response = call(client);

            assertThat(response).isEqualTo(new ProductResponse(200, "v1", "v1"));
        }

        @Test
        void should_run_the_product_plan_flow_of_the_new_version_after_an_update(HttpClient client) {
            allowProductKey();
            deployProductVersion(1);
            deployProductVersion(2);

            assertThat(call(client)).isEqualTo(new ProductResponse(200, "v2", "v2"));
        }

        @Test
        void should_hold_the_product_plan_flow_through_20_updates_under_traffic(HttpClient client) throws Exception {
            allowProductKey();
            deployProductVersion(0);
            List<ProductResponse> responses = new CopyOnWriteArrayList<>();
            AtomicBoolean updating = new AtomicBoolean(true);

            var traffic = Flowable.range(0, 4)
                .flatMap(
                    worker ->
                        Flowable.fromCallable(() -> call(client))
                            .repeatUntil(() -> !updating.get())
                            .subscribeOn(Schedulers.io()),
                    4
                )
                .doOnNext(responses::add)
                .ignoreElements()
                .test();

            awaitServedBy(responses, "v0");
            for (int version = 1; version <= 20; version++) {
                deployProductVersion(version);
                awaitServedBy(responses, "v" + version);
            }
            updating.set(false);
            traffic.await(30, TimeUnit.SECONDS);
            traffic.assertNoErrors().assertComplete();

            assertThat(responses)
                .extracting(ProductResponse::responseVersion)
                .containsAll(
                    IntStream.rangeClosed(0, 20)
                        .mapToObj(version -> "v" + version)
                        .toList()
                );
            assertThat(responses).allSatisfy(response -> {
                assertThat(response.status()).isEqualTo(200);
                assertThat(response.responseVersion()).isNotNull();
                assertThat(response.echoedVersion()).isEqualTo(response.responseVersion());
            });
            assertThat(call(client)).isEqualTo(new ProductResponse(200, "v20", "v20"));
        }
    }

    @Nested
    @GatewayTest
    @DeployApi({ "/apis/v4/http/api-product/api-1.json" })
    class ProductSecurityFromTheRuntime extends RuntimeTestPreparer {

        private static final String REMOVED_PLAN_ID = "runtime-product-removed-plan";
        private static final String REMOVED_PLAN_KEY = "runtime-product-removed-plan-key";

        @Test
        void should_accept_the_product_key_through_20_product_updates_under_traffic(HttpClient client) throws Exception {
            allowProductKeyOnPlan(KEY, PRODUCT_PLAN_ID);
            deployProductVersionWithPlans(0, List.of(PRODUCT_PLAN_ID));
            List<ProductResponse> responses = new CopyOnWriteArrayList<>();
            AtomicBoolean updating = new AtomicBoolean(true);

            var traffic = Flowable.range(0, 4)
                .flatMap(
                    worker ->
                        Flowable.fromCallable(() -> call(client))
                            .repeatUntil(() -> !updating.get())
                            .subscribeOn(Schedulers.io()),
                    4
                )
                .doOnNext(responses::add)
                .ignoreElements()
                .test();

            awaitServedBy(responses, "v0");
            for (int version = 1; version <= 20; version++) {
                deployProductVersionWithPlans(version, List.of(PRODUCT_PLAN_ID));
                awaitServedBy(responses, "v" + version);
            }
            updating.set(false);
            traffic.await(30, TimeUnit.SECONDS);
            traffic.assertNoErrors().assertComplete();

            assertThat(responses)
                .isNotEmpty()
                .allSatisfy(response -> assertThat(response.status()).isEqualTo(200));
        }

        @Test
        void should_reject_the_key_of_a_plan_the_product_update_removed(HttpClient client) {
            allowProductKeyOnPlan(KEY, PRODUCT_PLAN_ID);
            allowProductKeyOnPlan(REMOVED_PLAN_KEY, REMOVED_PLAN_ID);
            deployProductVersionWithPlans(1, List.of(PRODUCT_PLAN_ID, REMOVED_PLAN_ID));
            assertThat(call(client, REMOVED_PLAN_KEY).status()).isEqualTo(200);

            deployProductVersionWithPlans(2, List.of(PRODUCT_PLAN_ID));

            assertThat(call(client, REMOVED_PLAN_KEY).status()).isEqualTo(401);
            assertThat(call(client, KEY)).isEqualTo(new ProductResponse(200, "v2", "v2"));
        }
    }

    @Nested
    @GatewayTest
    @DeployApi({ "/apis/v4/http/api-product/api-1.json" })
    class SubscriptionValidationOff extends RuntimeTestPreparer {

        @Override
        protected void configureGateway(GatewayConfigurationBuilder gatewayConfigurationBuilder) {
            super.configureGateway(gatewayConfigurationBuilder);
            gatewayConfigurationBuilder.set("api.validateSubscription", false);
        }

        @Test
        void should_run_the_product_plan_flow_of_the_matched_product_plan(HttpClient client) {
            allowProductKey();
            deployProductVersion(1);

            assertThat(call(client)).isEqualTo(new ProductResponse(200, "v1", "v1"));
        }
    }

    @Nested
    @GatewayTest
    @DeployApi({ "/apis/v4/http/api-product/api-1.json" })
    class BestMatchMemberApi extends RuntimeTestPreparer {

        @Override
        public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
            super.configureApi(api, definitionClass);
            FlowExecution bestMatch = new FlowExecution();
            bestMatch.setMode(FlowMode.BEST_MATCH);
            ((Api) api.getDefinition()).setFlowExecution(bestMatch);
        }

        @Test
        void should_run_product_plan_flows_in_default_mode_when_the_product_saved_no_mode(HttpClient client) {
            allowProductKey();
            deployProductWithTwoMatchingFlows();

            HttpClientResponse response = client
                .rxRequest(GET, API_1_PATH)
                .flatMap(request -> request.putHeader("X-Gravitee-Api-Key", KEY).rxSend())
                .blockingGet();

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.getHeader(FIRST_FLOW_HEADER)).isEqualTo("ran");
            assertThat(response.getHeader(SECOND_FLOW_HEADER)).isEqualTo("ran");
        }
    }

    @Nested
    @GatewayTest
    @DeployApi({ "/apis/v4/http/api-product/api-1.json" })
    class SubscriptionValidationOffOnTheApiPlan extends RuntimeTestPreparer {

        @Override
        protected void configureGateway(GatewayConfigurationBuilder gatewayConfigurationBuilder) {
            super.configureGateway(gatewayConfigurationBuilder);
            gatewayConfigurationBuilder.set("api.validateSubscription", false);
        }

        @Override
        public void configurePolicies(Map<String, PolicyPlugin> policies) {
            super.configurePolicies(policies);
            policies.put("key-less", PolicyBuilder.build("key-less", KeylessPolicy.class));
        }

        @Override
        public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
            configurePlans((Api) api.getDefinition(), Set.of("key-less"));
        }

        @Test
        void should_run_no_product_flow_for_a_request_on_the_api_own_plan(HttpClient client) {
            allowProductKey();
            deployProductVersion(1);

            assertThat(call(client, null)).isEqualTo(new ProductResponse(200, null, null));
            assertThat(call(client, KEY)).isEqualTo(new ProductResponse(200, "v1", "v1"));
        }
    }

    @Nested
    @GatewayTest
    @DeployApi({ "/apis/v4/http/api-product/api-1.json" })
    class PublicAttributesRewrittenByAPolicy extends RuntimeTestPreparer {

        private static final String OTHER_PRODUCT_ID = "runtime-product-other";
        private static final String OTHER_PRODUCT_PLAN_ID = "runtime-product-other-plan";

        @Override
        public void configurePolicies(Map<String, PolicyPlugin> policies) {
            super.configurePolicies(policies);
            policies.put(
                "assign-attributes",
                PolicyBuilder.build("assign-attributes", AssignAttributesPolicy.class, AssignAttributesPolicyConfiguration.class)
            );
        }

        @Override
        public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
            super.configureApi(api, definitionClass);
            HttpSelector selector = new HttpSelector();
            selector.setPath("/");
            Step rewrite = Step.builder()
                .name("point the public attributes at another product")
                .enabled(true)
                .policy("assign-attributes")
                .configuration(
                    "{\"scope\":\"REQUEST\",\"attributes\":[" +
                        "{\"name\":\"gravitee.attribute.apiProduct\",\"value\":\"" +
                        OTHER_PRODUCT_ID +
                        "\"}," +
                        "{\"name\":\"gravitee.attribute.plan\",\"value\":\"" +
                        OTHER_PRODUCT_PLAN_ID +
                        "\"}]}"
                )
                .build();
            ((Api) api.getDefinition()).setFlows(
                List.of(Flow.builder().name("api flow").enabled(true).selectors(List.of(selector)).request(List.of(rewrite)).build())
            );
        }

        @Test
        void should_replay_the_subscribed_product_in_the_response_whatever_a_policy_sets_in_the_public_attributes(HttpClient client) {
            allowProductKey();
            deployProduct(PRODUCT_ID, PRODUCT_PLAN_ID, "subscribed");
            deployProduct(OTHER_PRODUCT_ID, OTHER_PRODUCT_PLAN_ID, "other");

            assertThat(call(client)).isEqualTo(new ProductResponse(200, "subscribed", "subscribed"));
        }
    }
}
