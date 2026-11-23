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

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.JWT_CLIENT_ID;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.JWT_SECRET;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.OAUTH2_CLIENT_ID;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.OAUTH2_SUCCESS_TOKEN;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.generateJWT;
import static io.gravitee.apim.integration.tests.plan.oauth2.MockOAuth2Resource.RESOURCE_ID;
import static io.vertx.core.http.HttpMethod.GET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.apim.gateway.tests.sdk.resource.ResourceBuilder;
import io.gravitee.apim.integration.tests.plan.oauth2.MockOAuth2Resource;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.execution.FlowExecution;
import io.gravitee.definition.model.v4.flow.execution.FlowMode;
import io.gravitee.definition.model.v4.flow.selector.ConditionSelector;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.gateway.api.service.ApiKey;
import io.gravitee.gateway.api.service.ApiKeyService;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.api.service.SubscriptionService;
import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.gravitee.gateway.reactive.api.policy.SecurityToken;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.reactor.handler.HttpAcceptor;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import io.gravitee.gateway.reactor.handler.ReactorHandlerRegistry;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.plugin.resource.ResourcePlugin;
import io.gravitee.policy.assignattributes.AssignAttributesPolicy;
import io.gravitee.policy.assignattributes.configuration.AssignAttributesPolicyConfiguration;
import io.gravitee.policy.jwt.alg.Signature;
import io.gravitee.policy.jwt.configuration.JWTPolicyConfiguration;
import io.gravitee.policy.oauth2.Oauth2Policy;
import io.gravitee.policy.oauth2.configuration.OAuth2PolicyConfiguration;
import io.gravitee.policy.ratelimit.RateLimitPolicy;
import io.gravitee.policy.ratelimit.configuration.RateLimitPolicyConfiguration;
import io.gravitee.policy.transformheaders.TransformHeadersPolicy;
import io.gravitee.policy.transformheaders.configuration.TransformHeadersPolicyConfiguration;
import io.gravitee.policy.v3.jwt.resolver.KeyResolver;
import io.gravitee.repository.ratelimit.api.RateLimitService;
import io.gravitee.repository.ratelimit.model.RateLimit;
import io.reactivex.rxjava3.core.Single;
import io.vertx.rxjava3.core.http.HttpClient;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Product flows run on every member API, for the subscribers of that product only, between the product plan flows and
 * the API plan flows, in both phases.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiProductV4FlowsIntegrationTest {

    private static final String PRODUCT_ID = "flows-product";
    private static final String OTHER_PRODUCT_ID = "flows-product-other";
    private static final String API_KEY_PLAN_ID = "flows-product-api-key-plan";
    private static final String OTHER_API_KEY_PLAN_ID = "flows-product-other-api-key-plan";
    private static final String JWT_PLAN_ID = "flows-product-jwt-plan";
    private static final String OAUTH2_PLAN_ID = "flows-product-oauth2-plan";
    private static final String API_1_ID = "my-api-v4-1";
    private static final String API_2_ID = "my-api-v4-2";
    private static final String API_1_PATH = "/test-1";
    private static final String API_2_PATH = "/test-2";
    private static final String KEY = "flows-product-key";
    private static final String OTHER_KEY = "flows-product-other-key";
    private static final String API_PLAN_KEY = "flows-api-plan-key";
    private static final String PRODUCT_HEADER = "X-Product-Flow";
    private static final String API_PLAN_HEADER = "X-Api-Plan-Flow";
    private static final String ORDER_ID_HEADER = "X-Order-Id";

    abstract static class FlowsTestPreparer extends ApiProductV4IntegrationTest.TestPreparer {

        @BeforeEach
        void stubAnyEndpoint() {
            wiremock.stubFor(get(anyUrl()).willReturn(ok("endpoint response")));
        }

        @Override
        public void configurePolicies(Map<String, PolicyPlugin> policies) {
            super.configurePolicies(policies);
            policies.put(
                "transform-headers",
                PolicyBuilder.build("transform-headers", TransformHeadersPolicy.class, TransformHeadersPolicyConfiguration.class)
            );
        }

        void deployProduct(String productId, String planId, List<Flow> productFlows) {
            deployProduct(productId, Set.of(API_1_ID, API_2_ID), productPlan(planId, "api-key", PlanStatus.PUBLISHED), productFlows, null);
        }

        void deployProduct(String productId, Set<String> apiIds, Plan plan, List<Flow> productFlows, FlowExecution flowExecution) {
            ReactableApiProduct product = product(productId, apiIds);
            product.setPlans(List.of(plan));
            product.setFlows(productFlows);
            product.setFlowExecution(flowExecution);
            deployApiProduct(product);
        }

        void allowProductKey(String productId, String planId, String key) {
            Subscription subscription = new Subscription();
            subscription.setId("subscription-" + key);
            subscription.setApplication("application-id");
            subscription.setApiProductId(productId);
            subscription.setPlan(planId);
            subscription.setStatus("ACCEPTED");
            for (String apiId : List.of(API_1_ID, API_2_ID)) {
                ApiKey apiKey = new ApiKey();
                apiKey.setApi(productId);
                apiKey.setApplication("application-id");
                apiKey.setSubscription(subscription.getId());
                apiKey.setPlan(planId);
                apiKey.setKey(key);
                apiKey.setActive(true);
                apiKey.setExpireAt(new Date(System.currentTimeMillis() + 600_000));
                when(getBean(ApiKeyService.class).getByApiAndKey(eq(apiId), eq(key))).thenReturn(Optional.of(apiKey));
            }
            allowSubscription(productId, planId, key, subscription);
        }

        void allowClientId(String productId, String planId, String clientId) {
            Subscription subscription = new Subscription();
            subscription.setId("subscription-" + clientId);
            subscription.setApplication("application-id");
            subscription.setApiProductId(productId);
            subscription.setPlan(planId);
            subscription.setClientId(clientId);
            subscription.setStatus("ACCEPTED");
            allowSubscription(productId, planId, clientId, subscription);
        }

        private void allowSubscription(String productId, String planId, String tokenValue, Subscription subscription) {
            when(
                getBean(SubscriptionService.class).getByApiAndSecurityToken(
                    eq(productId),
                    argThat((SecurityToken token) -> token != null && tokenValue.equals(token.getTokenValue())),
                    eq(planId)
                )
            ).thenReturn(Optional.of(subscription));
        }

        Response call(HttpClient client, String path, String headerName, String headerValue) {
            return client
                .rxRequest(GET, path)
                .flatMap(request -> (headerName == null ? request : request.putHeader(headerName, headerValue)).rxSend())
                .flatMap(response -> response.rxBody().map(body -> new Response(response.statusCode(), toMap(response.headers()))))
                .blockingGet();
        }

        Response callWithKey(HttpClient client, String path, String key) {
            return call(client, path, "X-Gravitee-Api-Key", key);
        }

        private static Map<String, String> toMap(io.vertx.core.MultiMap headers) {
            Map<String, String> map = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            headers.forEach(entry -> map.putIfAbsent(entry.getKey(), entry.getValue()));
            return map;
        }

        static Flow flow(String name, String path, List<Step> request, List<Step> response) {
            HttpSelector selector = new HttpSelector();
            selector.setPath(path);
            return Flow.builder().name(name).enabled(true).selectors(List.of(selector)).request(request).response(response).build();
        }

        /** Adds a header to the request or to the response, depending on the phase of the flow the step is in. */
        static Step addHeader(String name, String value) {
            return Step.builder()
                .name("add " + name)
                .enabled(true)
                .policy("transform-headers")
                .configuration("{\"addHeaders\":[{\"name\":\"" + name + "\",\"value\":\"" + value + "\"}]}")
                .build();
        }

        static FlowExecution matchRequired() {
            FlowExecution flowExecution = new FlowExecution();
            flowExecution.setMatchRequired(true);
            return flowExecution;
        }
    }

    record Response(int status, Map<String, String> headers) {
        String header(String name) {
            return headers.get(name);
        }
    }

    /** Counts in memory, as a gateway with a rate-limit repository does; the test harness only ships a no-op one. */
    static class InMemoryRateLimitService implements RateLimitService {

        private final Map<String, RateLimit> counters = new ConcurrentHashMap<>();

        @Override
        public Single<RateLimit> incrementAndGet(String key, long weight, boolean async, Supplier<RateLimit> supplier) {
            return Single.fromCallable(() ->
                counters.compute(key, (k, current) -> {
                    RateLimit rateLimit = current == null || current.getResetTime() <= System.currentTimeMillis()
                        ? supplier.get()
                        : current;
                    rateLimit.setCounter(rateLimit.getCounter() + weight);
                    return new RateLimit(rateLimit);
                })
            );
        }
    }

    @Nested
    @GatewayTest
    @DeployApi({ "/apis/v4/http/api-product/api-1.json", "/apis/v4/http/api-product/api-2.json" })
    class SharedRateLimit extends FlowsTestPreparer {

        @BeforeEach
        void installRateLimitService() {
            DefaultListableBeanFactory beanFactory =
                (DefaultListableBeanFactory) ((ConfigurableApplicationContext) applicationContext).getBeanFactory();
            String beanName = "inMemoryRateLimitService";
            if (beanFactory.containsSingleton(beanName)) {
                beanFactory.destroySingleton(beanName);
            }
            beanFactory.registerSingleton(beanName, new InMemoryRateLimitService());
        }

        @Override
        public void configureResources(Map<String, ResourcePlugin> resources) {
            resources.put(RESOURCE_ID, ResourceBuilder.build(RESOURCE_ID, MockOAuth2Resource.class));
        }

        @Override
        public void configurePolicies(Map<String, PolicyPlugin> policies) {
            super.configurePolicies(policies);
            policies.put("rate-limit", PolicyBuilder.build("rate-limit", RateLimitPolicy.class, RateLimitPolicyConfiguration.class));
            policies.put("oauth2", PolicyBuilder.build("oauth2", Oauth2Policy.class, OAuth2PolicyConfiguration.class));
        }

        @Test
        void should_share_the_rate_limit_of_a_product_flow_across_member_apis_with_an_api_key_plan(HttpClient client) {
            deployProduct(
                PRODUCT_ID,
                Set.of(API_1_ID, API_2_ID),
                productPlan(API_KEY_PLAN_ID, "api-key", PlanStatus.PUBLISHED),
                List.of(rateLimitFlow()),
                null
            );
            allowProductKey(PRODUCT_ID, API_KEY_PLAN_ID, KEY);

            assertThat(callWithKey(client, API_1_PATH, KEY).status()).isEqualTo(200);
            assertThat(callWithKey(client, API_2_PATH, KEY).status()).isEqualTo(200);
            assertThat(callWithKey(client, API_1_PATH, KEY).status()).isEqualTo(429);
            assertThat(callWithKey(client, API_2_PATH, KEY).status()).isEqualTo(429);
        }

        @Test
        void should_share_the_rate_limit_of_a_product_flow_across_member_apis_with_a_jwt_plan(HttpClient client) throws Exception {
            deployProduct(PRODUCT_ID, Set.of(API_1_ID, API_2_ID), jwtPlan(), List.of(rateLimitFlow()), null);
            allowClientId(PRODUCT_ID, JWT_PLAN_ID, JWT_CLIENT_ID);
            String bearer = "Bearer " + generateJWT(5000);

            assertThat(call(client, API_1_PATH, "Authorization", bearer).status()).isEqualTo(200);
            assertThat(call(client, API_2_PATH, "Authorization", bearer).status()).isEqualTo(200);
            assertThat(call(client, API_1_PATH, "Authorization", bearer).status()).isEqualTo(429);
            assertThat(call(client, API_2_PATH, "Authorization", bearer).status()).isEqualTo(429);
        }

        @Test
        void should_share_the_rate_limit_of_a_product_flow_across_member_apis_with_an_oauth2_plan(HttpClient client) {
            ReactableApiProduct deployed = product(PRODUCT_ID, Set.of(API_1_ID, API_2_ID));
            deployed.setPlans(List.of(oauth2Plan()));
            deployed.setFlows(List.of(rateLimitFlow()));
            deployed.setResources(List.of(Resource.builder().name(RESOURCE_ID).type(RESOURCE_ID).enabled(true).build()));
            deployApiProduct(deployed);
            allowClientId(PRODUCT_ID, OAUTH2_PLAN_ID, OAUTH2_CLIENT_ID);
            String bearer = "Bearer " + OAUTH2_SUCCESS_TOKEN;

            assertThat(call(client, API_1_PATH, "Authorization", bearer).status()).isEqualTo(200);
            assertThat(call(client, API_2_PATH, "Authorization", bearer).status()).isEqualTo(200);
            assertThat(call(client, API_1_PATH, "Authorization", bearer).status()).isEqualTo(429);
            assertThat(call(client, API_2_PATH, "Authorization", bearer).status()).isEqualTo(429);
        }

        private Flow rateLimitFlow() {
            Step rateLimit = Step.builder()
                .name("two calls a minute")
                .enabled(true)
                .policy("rate-limit")
                .configuration(
                    "{\"async\":false,\"addHeaders\":true,\"rate\":{\"limit\":2,\"periodTime\":1,\"periodTimeUnit\":\"MINUTES\"}}"
                )
                .build();
            return flow("product rate limit", "/", List.of(rateLimit), List.of());
        }

        private Plan jwtPlan() {
            JWTPolicyConfiguration configuration = new JWTPolicyConfiguration();
            configuration.setSignature(Signature.HMAC_HS256);
            configuration.setPublicKeyResolver(KeyResolver.GIVEN_KEY);
            configuration.setResolverParameter(JWT_SECRET);
            return securedPlan(JWT_PLAN_ID, "jwt", configuration);
        }

        private Plan oauth2Plan() {
            OAuth2PolicyConfiguration configuration = new OAuth2PolicyConfiguration();
            configuration.setOauthResource(RESOURCE_ID);
            return securedPlan(OAUTH2_PLAN_ID, "oauth2", configuration);
        }

        private Plan securedPlan(String planId, String type, Object configuration) {
            try {
                return Plan.builder()
                    .id(planId)
                    .name("ProductPlan-" + planId)
                    .security(PlanSecurity.builder().type(type).configuration(new ObjectMapper().writeValueAsString(configuration)).build())
                    .status(PlanStatus.PUBLISHED)
                    .mode(PlanMode.STANDARD)
                    .build();
            } catch (JsonProcessingException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @Nested
    @GatewayTest
    @DeployApi({ "/apis/v4/http/api-product/api-1.json", "/apis/v4/http/api-product/api-2.json" })
    class ExecutionOrder extends FlowsTestPreparer {

        @Override
        public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
            super.configureApi(api, definitionClass);
            ((Api) api.getDefinition()).setFlows(
                List.of(
                    flow(
                        "api flow",
                        "/",
                        List.of(addHeader("X-Request-3", "{#request.headers['X-Request-2'][0]}>api")),
                        List.of(
                            addHeader("X-Response-3", "{#response.headers['X-Response-2'][0]}>api"),
                            addHeader("X-Request-Trail", "{#request.headers['X-Request-3'][0]}")
                        )
                    )
                )
            );
        }

        @Test
        void should_run_product_plan_then_product_then_api_flows_in_both_phases(HttpClient client) {
            Plan plan = productPlan(API_KEY_PLAN_ID, "api-key", PlanStatus.PUBLISHED);
            plan.setFlows(
                List.of(
                    flow(
                        "product plan flow",
                        "/",
                        List.of(addHeader("X-Request-1", "product-plan")),
                        List.of(addHeader("X-Response-1", "product-plan"))
                    )
                )
            );
            Flow productFlow = flow(
                "product flow",
                "/",
                List.of(addHeader("X-Request-2", "{#request.headers['X-Request-1'][0]}>product")),
                List.of(addHeader("X-Response-2", "{#response.headers['X-Response-1'][0]}>product"))
            );
            deployProduct(PRODUCT_ID, Set.of(API_1_ID, API_2_ID), plan, List.of(productFlow), null);
            allowProductKey(PRODUCT_ID, API_KEY_PLAN_ID, KEY);

            Response response = callWithKey(client, API_1_PATH, KEY);

            assertThat(response.status()).isEqualTo(200);
            assertThat(response.header("X-Request-Trail")).isEqualTo("product-plan>product>api");
            assertThat(response.header("X-Response-3")).isEqualTo("product-plan>product>api");
        }
    }

    @Nested
    @GatewayTest
    @DeployApi({ "/apis/v4/http/api-product/api-1.json", "/apis/v4/http/api-product/api-2.json" })
    class ProductScope extends FlowsTestPreparer {

        @Test
        void should_run_only_the_flows_of_the_subscribed_product_when_two_products_share_an_api(HttpClient client) {
            deployProduct(PRODUCT_ID, API_KEY_PLAN_ID, List.of(productHeaderFlow("subscribed")));
            deployProduct(OTHER_PRODUCT_ID, OTHER_API_KEY_PLAN_ID, List.of(productHeaderFlow("other")));
            allowProductKey(PRODUCT_ID, API_KEY_PLAN_ID, KEY);
            allowProductKey(OTHER_PRODUCT_ID, OTHER_API_KEY_PLAN_ID, OTHER_KEY);

            assertThat(callWithKey(client, API_1_PATH, KEY).header(PRODUCT_HEADER)).isEqualTo("subscribed");
            assertThat(callWithKey(client, API_1_PATH, OTHER_KEY).header(PRODUCT_HEADER)).isEqualTo("other");
        }

        @Test
        void should_run_no_product_flow_on_a_direct_call_with_the_api_own_plan(HttpClient client) {
            deployProduct(PRODUCT_ID, API_KEY_PLAN_ID, List.of(productHeaderFlow("subscribed")));
            allowKeyForApi(API_PLAN_KEY, API_1_ID);

            Response response = callWithKey(client, API_1_PATH, API_PLAN_KEY);

            assertThat(response.status()).isEqualTo(200);
            assertThat(response.header(PRODUCT_HEADER)).isNull();
        }

        @Test
        void should_stop_running_product_flows_on_an_api_removed_from_the_product(HttpClient client) {
            deployProduct(PRODUCT_ID, API_KEY_PLAN_ID, List.of(productHeaderFlow("subscribed")));
            allowProductKey(PRODUCT_ID, API_KEY_PLAN_ID, KEY);
            assertThat(callWithKey(client, API_2_PATH, KEY).header(PRODUCT_HEADER)).isEqualTo("subscribed");

            deployProduct(
                PRODUCT_ID,
                Set.of(API_1_ID),
                productPlan(API_KEY_PLAN_ID, "api-key", PlanStatus.PUBLISHED),
                List.of(productHeaderFlow("subscribed")),
                null
            );

            assertThat(callWithKey(client, API_1_PATH, KEY).header(PRODUCT_HEADER)).isEqualTo("subscribed");
            assertThat(callWithKey(client, API_2_PATH, KEY).header(PRODUCT_HEADER)).isNull();
        }

        @Test
        void should_run_the_product_flows_of_an_update_without_redeploying_member_apis(HttpClient client) {
            deployProduct(PRODUCT_ID, API_KEY_PLAN_ID, List.of(productHeaderFlow("v1")));
            allowProductKey(PRODUCT_ID, API_KEY_PLAN_ID, KEY);
            assertThat(callWithKey(client, API_1_PATH, KEY).header(PRODUCT_HEADER)).isEqualTo("v1");
            List<ReactorHandler> reactorsBefore = memberApiReactors();

            deployProduct(PRODUCT_ID, API_KEY_PLAN_ID, List.of(productHeaderFlow("v2")));

            assertThat(callWithKey(client, API_1_PATH, KEY).header(PRODUCT_HEADER)).isEqualTo("v2");
            assertThat(callWithKey(client, API_2_PATH, KEY)).satisfies(response -> {
                assertThat(response.status()).isEqualTo(200);
                assertThat(response.header(PRODUCT_HEADER)).isEqualTo("v2");
            });
            assertThat(memberApiReactors()).containsExactlyInAnyOrderElementsOf(reactorsBefore);
        }

        @Test
        void should_start_running_product_flows_on_an_api_added_to_the_product(HttpClient client) {
            deployProduct(
                PRODUCT_ID,
                Set.of(API_1_ID),
                productPlan(API_KEY_PLAN_ID, "api-key", PlanStatus.PUBLISHED),
                List.of(productHeaderFlow("subscribed")),
                null
            );
            allowProductKey(PRODUCT_ID, API_KEY_PLAN_ID, KEY);
            assertThat(callWithKey(client, API_2_PATH, KEY).header(PRODUCT_HEADER)).isNull();

            deployProduct(PRODUCT_ID, API_KEY_PLAN_ID, List.of(productHeaderFlow("subscribed")));

            assertThat(callWithKey(client, API_2_PATH, KEY).header(PRODUCT_HEADER)).isEqualTo("subscribed");
        }

        private List<ReactorHandler> memberApiReactors() {
            return getBean(ReactorHandlerRegistry.class)
                .getAcceptors(HttpAcceptor.class)
                .stream()
                .map(HttpAcceptor::reactor)
                .distinct()
                .toList();
        }

        @Test
        void should_read_a_path_parameter_of_a_product_flow_on_member_apis_with_different_context_paths(HttpClient client) {
            Flow orderFlow = flow("order", "/orders/:id", List.of(), List.of(addHeader(ORDER_ID_HEADER, "{#request.pathParams['id']}")));
            deployProduct(PRODUCT_ID, API_KEY_PLAN_ID, List.of(orderFlow));
            allowProductKey(PRODUCT_ID, API_KEY_PLAN_ID, KEY);

            assertThat(callWithKey(client, API_1_PATH + "/orders/42", KEY).header(ORDER_ID_HEADER)).isEqualTo("42");
            assertThat(callWithKey(client, API_2_PATH + "/orders/7", KEY).header(ORDER_ID_HEADER)).isEqualTo("7");
            assertThat(callWithKey(client, API_1_PATH + "/invoices/42", KEY).header(ORDER_ID_HEADER)).isNull();
        }
    }

    @Nested
    @GatewayTest
    @DeployApi({ "/apis/v4/http/api-product/api-1.json", "/apis/v4/http/api-product/api-2.json" })
    class FlowModePerProduct extends FlowsTestPreparer {

        private static final String ROOT_HEADER = "X-Root-Flow";
        private static final String ORDERS_HEADER = "X-Orders-Flow";
        private static final String ORDERS_ON_HEADER = "X-Orders-On";

        @Test
        void should_select_the_product_flows_in_the_flow_mode_of_the_subscribed_product(HttpClient client) {
            List<Flow> flows = List.of(
                flow("root", "/", List.of(), List.of(addHeader(ROOT_HEADER, "ran"))),
                flow("orders", "/orders", List.of(), List.of(addHeader(ORDERS_HEADER, "ran")))
            );
            deployProductsInBothModes(flows);

            assertThat(callWithKey(client, API_1_PATH + "/orders", KEY)).satisfies(response -> {
                assertThat(response.header(ROOT_HEADER)).isEqualTo("ran");
                assertThat(response.header(ORDERS_HEADER)).isEqualTo("ran");
            });
            assertThat(callWithKey(client, API_1_PATH + "/orders", OTHER_KEY)).satisfies(response -> {
                assertThat(response.header(ROOT_HEADER)).isNull();
                assertThat(response.header(ORDERS_HEADER)).isEqualTo("ran");
            });
        }

        @Test
        void should_evaluate_the_product_flow_conditions_when_the_flow_mode_of_the_subscribed_product_says_so(HttpClient client) {
            Flow ordersWhenTurnedOn = flow("orders", "/orders", List.of(), List.of(addHeader(ORDERS_HEADER, "ran")));
            ConditionSelector turnedOn = new ConditionSelector();
            turnedOn.setCondition("{#request.headers['" + ORDERS_ON_HEADER + "'] != null}");
            ordersWhenTurnedOn.setSelectors(List.of(ordersWhenTurnedOn.getSelectors().get(0), turnedOn));
            deployProductsInBothModes(
                List.of(
                    flow("root", "/", List.of(addHeader(ORDERS_ON_HEADER, "true")), List.of(addHeader(ROOT_HEADER, "ran"))),
                    ordersWhenTurnedOn
                )
            );

            // DEFAULT evaluates the condition right before the flow runs, once the root flow has turned it on.
            assertThat(callWithKey(client, API_1_PATH + "/orders", KEY)).satisfies(response -> {
                assertThat(response.header(ROOT_HEADER)).isEqualTo("ran");
                assertThat(response.header(ORDERS_HEADER)).isEqualTo("ran");
            });
            // BEST_MATCH evaluates it while selecting the one flow to run, before any flow ran.
            assertThat(callWithKey(client, API_1_PATH + "/orders", OTHER_KEY)).satisfies(response -> {
                assertThat(response.header(ROOT_HEADER)).isEqualTo("ran");
                assertThat(response.header(ORDERS_HEADER)).isNull();
            });
        }

        private void deployProductsInBothModes(List<Flow> flows) {
            FlowExecution bestMatch = new FlowExecution();
            bestMatch.setMode(FlowMode.BEST_MATCH);
            deployProduct(PRODUCT_ID, Set.of(API_1_ID), productPlan(API_KEY_PLAN_ID, "api-key", PlanStatus.PUBLISHED), flows, null);
            deployProduct(
                OTHER_PRODUCT_ID,
                Set.of(API_1_ID),
                productPlan(OTHER_API_KEY_PLAN_ID, "api-key", PlanStatus.PUBLISHED),
                flows,
                bestMatch
            );
            allowProductKey(PRODUCT_ID, API_KEY_PLAN_ID, KEY);
            allowProductKey(OTHER_PRODUCT_ID, OTHER_API_KEY_PLAN_ID, OTHER_KEY);
        }
    }

    @Nested
    @GatewayTest
    @DeployApi({ "/apis/v4/http/api-product/api-1.json", "/apis/v4/http/api-product/api-2.json" })
    class PublicAttributesRewrittenByAPolicy extends FlowsTestPreparer {

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
                        OTHER_API_KEY_PLAN_ID +
                        "\"}]}"
                )
                .build();
            ((Api) api.getDefinition()).setFlows(List.of(flow("api flow", "/", List.of(rewrite), List.of())));
        }

        @Test
        void should_run_the_subscribed_product_flows_in_the_response_whatever_a_policy_sets_in_the_public_attributes(HttpClient client) {
            deployProduct(PRODUCT_ID, API_KEY_PLAN_ID, List.of(productHeaderFlow("subscribed")));
            deployProduct(OTHER_PRODUCT_ID, OTHER_API_KEY_PLAN_ID, List.of(productHeaderFlow("other")));
            allowProductKey(PRODUCT_ID, API_KEY_PLAN_ID, KEY);

            assertThat(callWithKey(client, API_1_PATH, KEY).header(PRODUCT_HEADER)).isEqualTo("subscribed");
        }
    }

    @Nested
    @GatewayTest
    @DeployApi({ "/apis/v4/http/api-product/api-1.json", "/apis/v4/http/api-product/api-2.json" })
    class ProductMatchRequired extends FlowsTestPreparer {

        @Test
        void should_return_404_when_neither_a_product_plan_flow_nor_a_product_flow_matched(HttpClient client) {
            deployProduct(
                PRODUCT_ID,
                Set.of(API_1_ID, API_2_ID),
                productPlan(API_KEY_PLAN_ID, "api-key", PlanStatus.PUBLISHED),
                List.of(flow("orders", "/orders", List.of(), List.of(addHeader(PRODUCT_HEADER, "orders")))),
                matchRequired()
            );
            allowProductKey(PRODUCT_ID, API_KEY_PLAN_ID, KEY);

            assertThat(callWithKey(client, API_1_PATH + "/orders", KEY).status()).isEqualTo(200);
            assertThat(callWithKey(client, API_1_PATH + "/invoices", KEY).status()).isEqualTo(404);
            assertThat(callWithKey(client, API_2_PATH + "/invoices", KEY).status()).isEqualTo(404);
        }

        @Test
        void should_not_apply_the_match_required_of_another_product_sharing_the_api(HttpClient client) {
            deployProduct(
                PRODUCT_ID,
                Set.of(API_1_ID),
                productPlan(API_KEY_PLAN_ID, "api-key", PlanStatus.PUBLISHED),
                List.of(flow("orders", "/orders", List.of(), List.of())),
                matchRequired()
            );
            deployProduct(OTHER_PRODUCT_ID, OTHER_API_KEY_PLAN_ID, List.of(flow("orders", "/orders", List.of(), List.of())));
            allowProductKey(PRODUCT_ID, API_KEY_PLAN_ID, KEY);
            allowProductKey(OTHER_PRODUCT_ID, OTHER_API_KEY_PLAN_ID, OTHER_KEY);

            assertThat(callWithKey(client, API_1_PATH + "/invoices", KEY).status()).isEqualTo(404);
            assertThat(callWithKey(client, API_1_PATH + "/invoices", OTHER_KEY).status()).isEqualTo(200);
        }

        @Test
        void should_satisfy_the_product_match_required_with_a_product_plan_flow(HttpClient client) {
            Plan plan = productPlan(API_KEY_PLAN_ID, "api-key", PlanStatus.PUBLISHED);
            plan.setFlows(List.of(flow("everything", "/", List.of(), List.of())));
            deployProduct(
                PRODUCT_ID,
                Set.of(API_1_ID, API_2_ID),
                plan,
                List.of(flow("orders", "/orders", List.of(), List.of())),
                matchRequired()
            );
            allowProductKey(PRODUCT_ID, API_KEY_PLAN_ID, KEY);

            assertThat(callWithKey(client, API_1_PATH + "/invoices", KEY).status()).isEqualTo(200);
        }
    }

    @Nested
    @GatewayTest
    @DeployApi({ "/apis/v4/http/api-product/api-1.json", "/apis/v4/http/api-product/api-2.json" })
    class ApiMatchRequired extends FlowsTestPreparer {

        @Override
        public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
            super.configureApi(api, definitionClass);
            Api definition = (Api) api.getDefinition();
            definition.setFlowExecution(matchRequired());
            definition.setFlows(List.of(flow("api orders", "/api-orders", List.of(), List.of())));
        }

        @Test
        void should_count_a_matching_product_flow_for_the_api_match_required(HttpClient client) {
            deployProduct(PRODUCT_ID, API_KEY_PLAN_ID, List.of(flow("orders", "/orders", List.of(), List.of())));
            allowProductKey(PRODUCT_ID, API_KEY_PLAN_ID, KEY);

            assertThat(callWithKey(client, API_1_PATH + "/orders", KEY).status()).isEqualTo(200);
            assertThat(callWithKey(client, API_1_PATH + "/invoices", KEY).status()).isEqualTo(404);
        }
    }

    @Nested
    @GatewayTest
    @DeployApi({ "/apis/v4/http/api-product/api-1.json", "/apis/v4/http/api-product/api-2.json" })
    class SubscriptionValidationOff extends FlowsTestPreparer {

        @Override
        protected void configureGateway(GatewayConfigurationBuilder gatewayConfigurationBuilder) {
            super.configureGateway(gatewayConfigurationBuilder);
            gatewayConfigurationBuilder.set("api.validateSubscription", false);
        }

        @Override
        public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
            super.configureApi(api, definitionClass);
            ((Api) api.getDefinition()).getPlans().forEach(plan ->
                plan.setFlows(List.of(flow("api plan flow", "/", List.of(), List.of(addHeader(API_PLAN_HEADER, "ran")))))
            );
        }

        @Test
        void should_run_the_product_flows_of_the_matched_product_plan(HttpClient client) {
            deployProduct(PRODUCT_ID, API_KEY_PLAN_ID, List.of(productHeaderFlow("matched")));
            allowProductKey(PRODUCT_ID, API_KEY_PLAN_ID, KEY);

            assertThat(callWithKey(client, API_1_PATH, KEY).header(PRODUCT_HEADER)).isEqualTo("matched");
        }

        @Test
        void should_run_only_the_flows_of_the_subscribed_product_when_two_products_share_an_api(HttpClient client) {
            deployProduct(PRODUCT_ID, API_KEY_PLAN_ID, List.of(productHeaderFlow("subscribed")));
            deployProduct(OTHER_PRODUCT_ID, OTHER_API_KEY_PLAN_ID, List.of(productHeaderFlow("other")));
            allowProductKey(PRODUCT_ID, API_KEY_PLAN_ID, KEY);
            allowProductKey(OTHER_PRODUCT_ID, OTHER_API_KEY_PLAN_ID, OTHER_KEY);

            assertThat(callWithKey(client, API_1_PATH, KEY)).satisfies(response -> {
                assertThat(response.header(PRODUCT_HEADER)).isEqualTo("subscribed");
                assertThat(response.header(API_PLAN_HEADER)).isNull();
            });
            assertThat(callWithKey(client, API_1_PATH, OTHER_KEY)).satisfies(response -> {
                assertThat(response.header(PRODUCT_HEADER)).isEqualTo("other");
                assertThat(response.header(API_PLAN_HEADER)).isNull();
            });
        }

        @Test
        void should_run_the_api_plan_flows_and_no_product_flow_for_a_key_of_the_api_own_plan(HttpClient client) {
            deployProduct(PRODUCT_ID, API_KEY_PLAN_ID, List.of(productHeaderFlow("subscribed")));
            deployProduct(OTHER_PRODUCT_ID, OTHER_API_KEY_PLAN_ID, List.of(productHeaderFlow("other")));
            allowKeyForApi(API_PLAN_KEY, API_1_ID);

            Response response = callWithKey(client, API_1_PATH, API_PLAN_KEY);

            assertThat(response.status()).isEqualTo(200);
            assertThat(response.header(PRODUCT_HEADER)).isNull();
            assertThat(response.header(API_PLAN_HEADER)).isEqualTo("ran");
        }
    }

    private static Flow productHeaderFlow(String value) {
        return FlowsTestPreparer.flow("product flow " + value, "/", List.of(), List.of(FlowsTestPreparer.addHeader(PRODUCT_HEADER, value)));
    }
}
