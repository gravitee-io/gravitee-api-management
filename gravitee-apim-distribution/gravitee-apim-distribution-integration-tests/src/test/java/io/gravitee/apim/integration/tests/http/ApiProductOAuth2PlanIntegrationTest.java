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
import static io.gravitee.apim.integration.tests.plan.PlanHelper.OAUTH2_CLIENT_ID;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.OAUTH2_SUCCESS_TOKEN;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.OAUTH2_UNAUTHORIZED_TOKEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApiProducts;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.apim.gateway.tests.sdk.resource.ResourceBuilder;
import io.gravitee.apim.integration.tests.plan.oauth2.MockOAuth2Resource;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.api.service.SubscriptionService;
import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.gravitee.gateway.reactive.api.policy.SecurityToken;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.plugin.resource.ResourcePlugin;
import io.gravitee.policy.oauth2.Oauth2Policy;
import io.gravitee.policy.oauth2.configuration.OAuth2PolicyConfiguration;
import io.gravitee.resource.api.ResourceConfiguration;
import io.gravitee.resource.cache.api.Cache;
import io.gravitee.resource.cache.api.CacheResource;
import io.gravitee.resource.cache.api.Element;
import io.gravitee.resource.oauth2.api.OAuth2Resource;
import io.gravitee.resource.oauth2.api.OAuth2Response;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * OAuth2 plans of an API Product introspect with the product's resources, on every member API.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiProductOAuth2PlanIntegrationTest {

    private static final String ENV_ID = "DEFAULT";
    private static final String API_1_ID = "my-api-v4-1";
    private static final String API_2_ID = "my-api-v4-2";
    private static final String API_1_PATH = "/test-1";
    private static final String API_2_PATH = "/test-2";
    private static final String PRODUCT_ID = "oauth2-product";
    private static final String PLAN_ID = "oauth2-product-plan";
    private static final String RESOURCE_NAME = "prod-oauth";
    private static final String CACHE_NAME = "prod-cache";
    private static final String PRODUCT_OAUTH = "product-oauth2";
    private static final String ACCEPT_ALL_OAUTH = "accept-all-oauth2";
    private static final String PRODUCT_CACHE = "product-cache";

    @Nested
    @GatewayTest
    @DeployApiProducts({})
    @DeployApi({ "/apis/v4/http/api-product/api-1.json", "/apis/v4/http/api-product/api-2.json" })
    class ProductPlanUsesProductResources extends ProductSecurityPreparer {

        @BeforeEach
        void resetResources() {
            CountingOAuth2Resource.introspections.set(0);
            ProductCacheResource.clear();
        }

        @Override
        public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
            super.configureApi(api, definitionClass);
            if (!isV4Api(definitionClass)) {
                return;
            }
            Api definition = (Api) api.getDefinition();
            if (API_1_ID.equals(definition.getId())) {
                definition.setResources(List.of(resource(RESOURCE_NAME, ACCEPT_ALL_OAUTH, true)));
            }
        }

        @Test
        void should_accept_a_subscribed_client_on_every_member_and_reject_the_others(HttpClient client) {
            deploy(Set.of(API_1_ID, API_2_ID), true, true);
            allow(PRODUCT_ID, PLAN_ID, OAUTH2_CLIENT_ID);

            assertThat(call(client, API_1_PATH, OAUTH2_SUCCESS_TOKEN)).isEqualTo(200);
            assertThat(call(client, API_2_PATH, OAUTH2_SUCCESS_TOKEN)).isEqualTo(200);
            assertThat(call(client, API_1_PATH, OAUTH2_UNAUTHORIZED_TOKEN)).isEqualTo(401);
            assertThat(call(client, API_2_PATH, OAUTH2_UNAUTHORIZED_TOKEN)).isEqualTo(401);
            assertThat(call(client, API_1_PATH, null)).isEqualTo(401);
        }

        @Test
        void should_not_use_a_member_api_resource_with_the_same_name(HttpClient client) {
            deploy(Set.of(API_1_ID, API_2_ID), true, false);
            allow(PRODUCT_ID, PLAN_ID, OAUTH2_CLIENT_ID);

            assertThat(call(client, API_1_PATH, OAUTH2_UNAUTHORIZED_TOKEN)).isEqualTo(401);
            assertThat(call(client, API_2_PATH, OAUTH2_UNAUTHORIZED_TOKEN)).isEqualTo(401);
        }

        @Test
        void should_follow_membership_without_a_new_subscription(HttpClient client) {
            deploy(Set.of(API_1_ID), true, false);
            allow(PRODUCT_ID, PLAN_ID, OAUTH2_CLIENT_ID);
            assertThat(call(client, API_1_PATH, OAUTH2_SUCCESS_TOKEN)).isEqualTo(200);
            assertThat(call(client, API_2_PATH, OAUTH2_SUCCESS_TOKEN)).isEqualTo(401);

            redeploy(Set.of(API_1_ID, API_2_ID), true, false);
            assertThat(call(client, API_2_PATH, OAUTH2_SUCCESS_TOKEN)).isEqualTo(200);

            redeploy(Set.of(API_1_ID), true, false);
            assertThat(call(client, API_2_PATH, OAUTH2_SUCCESS_TOKEN)).isEqualTo(401);
        }

        @Test
        void should_skip_introspection_until_the_cache_entry_expires(HttpClient client) {
            deploy(Set.of(API_1_ID, API_2_ID), true, true);
            allow(PRODUCT_ID, PLAN_ID, OAUTH2_CLIENT_ID);

            assertThat(call(client, API_1_PATH, OAUTH2_SUCCESS_TOKEN)).isEqualTo(200);
            assertThat(call(client, API_2_PATH, OAUTH2_SUCCESS_TOKEN)).isEqualTo(200);
            assertThat(CountingOAuth2Resource.introspections).hasValue(1);

            ProductCacheResource.expire();
            assertThat(call(client, API_1_PATH, OAUTH2_SUCCESS_TOKEN)).isEqualTo(200);
            assertThat(CountingOAuth2Resource.introspections).hasValue(2);
        }

        @Test
        void should_reject_when_the_product_oauth2_resource_is_disabled(HttpClient client) {
            deploy(Set.of(API_1_ID, API_2_ID), false, false);
            allow(PRODUCT_ID, PLAN_ID, OAUTH2_CLIENT_ID);

            assertThat(call(client, API_1_PATH, OAUTH2_SUCCESS_TOKEN)).isEqualTo(401);
            assertThat(call(client, API_2_PATH, OAUTH2_SUCCESS_TOKEN)).isEqualTo(401);
        }

        @Test
        void should_reject_when_the_product_has_no_oauth2_resource(HttpClient client) {
            ReactableApiProduct product = oauthProduct(PRODUCT_ID, Set.of(API_1_ID, API_2_ID));
            product.setPlans(List.of(oauthPlan(PLAN_ID, false)));
            deployApiProduct(product);
            allow(PRODUCT_ID, PLAN_ID, OAUTH2_CLIENT_ID);

            assertThat(call(client, API_1_PATH, OAUTH2_SUCCESS_TOKEN)).isEqualTo(401);
            assertThat(call(client, API_2_PATH, OAUTH2_SUCCESS_TOKEN)).isEqualTo(401);
        }
    }

    @Nested
    @GatewayTest
    @DeployApiProducts({})
    @DeployApi({ "/apis/v4/http/api-product/api-1.json", "/apis/v4/http/api-product/api-2.json" })
    class MemberApiPlanUsesApiResources extends ProductSecurityPreparer {

        private static final String API_PLAN_ID = "api-oauth2-plan";

        @Override
        public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
            if (!isV4Api(definitionClass)) {
                return;
            }
            Api definition = (Api) api.getDefinition();
            if (API_1_ID.equals(definition.getId())) {
                definition.setResources(List.of(resource(RESOURCE_NAME, PRODUCT_OAUTH, true)));
                definition.setPlans(List.of(oauthPlan(API_PLAN_ID, false)));
            } else {
                definition.setPlans(List.of());
            }
        }

        @Test
        void should_keep_using_the_api_resource_when_the_product_has_the_same_name(HttpClient client) {
            ReactableApiProduct product = oauthProduct(PRODUCT_ID, Set.of(API_1_ID));
            product.setResources(List.of(resource(RESOURCE_NAME, ACCEPT_ALL_OAUTH, true)));
            deployApiProduct(product);
            allow(API_1_ID, API_PLAN_ID, OAUTH2_CLIENT_ID);

            assertThat(call(client, API_1_PATH, OAUTH2_UNAUTHORIZED_TOKEN)).isEqualTo(401);
            assertThat(call(client, API_1_PATH, OAUTH2_SUCCESS_TOKEN)).isEqualTo(200);
        }
    }

    abstract static class ProductSecurityPreparer extends ApiProductV4IntegrationTest.TestPreparer {

        private int deployments;

        @BeforeEach
        void stubEndpoint() {
            wiremock.stubFor(get(anyUrl()).willReturn(ok("endpoint response")));
        }

        @Override
        public void configurePolicies(Map<String, PolicyPlugin> policies) {
            super.configurePolicies(policies);
            policies.put("oauth2", PolicyBuilder.build("oauth2", Oauth2Policy.class, OAuth2PolicyConfiguration.class));
        }

        @Override
        public void configureResources(Map<String, ResourcePlugin> resources) {
            resources.put(PRODUCT_OAUTH, ResourceBuilder.build(PRODUCT_OAUTH, CountingOAuth2Resource.class));
            resources.put(ACCEPT_ALL_OAUTH, ResourceBuilder.build(ACCEPT_ALL_OAUTH, AcceptAllOAuth2Resource.class));
            resources.put(PRODUCT_CACHE, ResourceBuilder.build(PRODUCT_CACHE, ProductCacheResource.class));
        }

        @Override
        public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
            entrypoints.putIfAbsent("http-proxy", EntrypointBuilder.build("http-proxy", HttpProxyEntrypointConnectorFactory.class));
        }

        @Override
        public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
            endpoints.putIfAbsent("http-proxy", EndpointBuilder.build("http-proxy", HttpProxyEndpointConnectorFactory.class));
        }

        void deploy(Set<String> apiIds, boolean oauthEnabled, boolean withCache) {
            ReactableApiProduct product = oauthProduct(PRODUCT_ID, apiIds);
            product.setPlans(List.of(oauthPlan(PLAN_ID, withCache)));
            product.setResources(resources(oauthEnabled, withCache));
            product.setDeployedAt(new java.util.Date(++deployments));
            deployApiProduct(product);
        }

        void redeploy(Set<String> apiIds, boolean oauthEnabled, boolean withCache) {
            ReactableApiProduct product = oauthProduct(PRODUCT_ID, apiIds);
            product.setPlans(List.of(oauthPlan(PLAN_ID, withCache)));
            product.setResources(resources(oauthEnabled, withCache));
            product.setDeployedAt(new java.util.Date(++deployments));
            redeployApiProduct(product);
        }

        void allow(String referenceId, String planId, String clientId) {
            Subscription subscription = new Subscription();
            subscription.setId("subscription-" + planId);
            subscription.setApplication("application-id");
            subscription.setPlan(planId);
            subscription.setApiProductId(PRODUCT_ID.equals(referenceId) ? PRODUCT_ID : null);
            subscription.setStatus("ACCEPTED");
            when(
                getBean(SubscriptionService.class).getByApiAndSecurityToken(
                    eq(referenceId),
                    argThat(
                        token ->
                            token != null &&
                            SecurityToken.TokenType.CLIENT_ID.name().equals(token.getTokenType()) &&
                            clientId.equals(token.getTokenValue())
                    ),
                    eq(planId)
                )
            ).thenReturn(Optional.of(subscription));
        }

        int call(HttpClient client, String path, String token) {
            return client
                .rxRequest(HttpMethod.GET, path)
                .flatMap(request -> {
                    if (token != null) {
                        request.putHeader("Authorization", "Bearer " + token);
                    }
                    return request.rxSend();
                })
                .map(response -> response.statusCode())
                .blockingGet();
        }

        protected static List<Resource> resources(boolean oauthEnabled, boolean withCache) {
            List<Resource> resources = new java.util.ArrayList<>();
            resources.add(resource(RESOURCE_NAME, PRODUCT_OAUTH, oauthEnabled));
            if (withCache) {
                resources.add(resource(CACHE_NAME, PRODUCT_CACHE, true));
            }
            return resources;
        }

        protected static Resource resource(String name, String type, boolean enabled) {
            return Resource.builder().name(name).type(type).enabled(enabled).configuration("{}").build();
        }

        protected static Plan oauthPlan(String planId, boolean withCache) {
            OAuth2PolicyConfiguration configuration = new OAuth2PolicyConfiguration();
            configuration.setOauthResource(RESOURCE_NAME);
            if (withCache) {
                configuration.setOauthCacheResource(CACHE_NAME);
            }
            try {
                return Plan.builder()
                    .id(planId)
                    .name("OAuth2 " + planId)
                    .security(
                        PlanSecurity.builder().type("oauth2").configuration(new ObjectMapper().writeValueAsString(configuration)).build()
                    )
                    .status(PlanStatus.PUBLISHED)
                    .mode(PlanMode.STANDARD)
                    .build();
            } catch (JsonProcessingException e) {
                throw new IllegalStateException(e);
            }
        }

        protected static ReactableApiProduct oauthProduct(String productId, Set<String> apiIds) {
            return ReactableApiProduct.builder()
                .id(productId)
                .name("Product-" + productId)
                .version("1.0.0")
                .apiIds(apiIds)
                .environmentId(ENV_ID)
                .build();
        }
    }

    public static class CountingOAuth2Resource extends MockOAuth2Resource {

        static final AtomicInteger introspections = new AtomicInteger();

        @Override
        public void introspect(String accessToken, Handler<OAuth2Response> responseHandler) {
            introspections.incrementAndGet();
            super.introspect(accessToken, responseHandler);
        }
    }

    public static class AcceptAllOAuth2Resource extends OAuth2Resource<AcceptAllOAuth2Resource.Configuration> {

        @Override
        public void introspect(String accessToken, Handler<OAuth2Response> responseHandler) {
            responseHandler.handle(new OAuth2Response(true, "{ \"client_id\": \"" + OAUTH2_CLIENT_ID + "\"}"));
        }

        @Override
        public void userInfo(String accessToken, Handler<io.gravitee.resource.oauth2.api.openid.UserInfoResponse> responseHandler) {}

        @Override
        public io.gravitee.resource.oauth2.api.OAuth2ResourceMetadata getProtectedResourceMetadata(String protectedResourceUri) {
            return null;
        }

        public static class Configuration implements ResourceConfiguration {}
    }

    public static class ProductCacheResource extends CacheResource<ProductCacheResource.Configuration> {

        private static final Map<Object, Element> entries = new HashMap<>();
        private static volatile boolean expired;
        private static final Cache CACHE = new Cache() {
            @Override
            public String getName() {
                return CACHE_NAME;
            }

            @Override
            public Object getNativeCache() {
                return entries;
            }

            @Override
            public Element get(Object key) {
                return expired ? null : entries.get(key);
            }

            @Override
            public void put(Element element) {
                entries.put(element.key(), element);
            }

            @Override
            public void evict(Object key) {
                entries.remove(key);
            }

            @Override
            public void clear() {
                entries.clear();
            }
        };

        static void clear() {
            expired = false;
            entries.clear();
        }

        static void expire() {
            expired = true;
        }

        @Override
        public Cache getCache(io.gravitee.gateway.api.ExecutionContext ctx) {
            return CACHE;
        }

        @Override
        public Cache getCache(io.gravitee.gateway.reactive.api.context.GenericExecutionContext ctx) {
            return CACHE;
        }

        @Override
        public Cache getCache(io.gravitee.gateway.reactive.api.context.base.BaseExecutionContext ctx) {
            return CACHE;
        }

        public static class Configuration implements ResourceConfiguration {}
    }
}
