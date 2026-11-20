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

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.gravitee.apim.integration.tests.http.ApiProductV4FlowsIntegrationTest.FlowsTestPreparer.flow;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.PLAN_APIKEY_ID;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.apim.gateway.tests.sdk.resource.ResourceBuilder;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.plugin.resource.ResourcePlugin;
import io.gravitee.policy.cache.CachePolicy;
import io.gravitee.policy.cache.configuration.CachePolicyConfiguration;
import io.gravitee.resource.cache.NodeCacheResource;
import io.gravitee.resource.cache.configuration.CacheResourceConfiguration;
import io.vertx.rxjava3.core.http.HttpClient;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * The cache policy resolves its resource while the product chain runs, then reads it again from the invoker that calls
 * the backend. These scenarios use the real cache policy and the in-memory cache resource.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiProductV4CachePolicyIntegrationTest {

    private static final String PRODUCT_ID = "cache-product";
    private static final String PLAN_ID = "cache-product-plan";
    private static final String API_1_ID = "my-api-v4-1";
    private static final String API_2_ID = "my-api-v4-2";
    private static final String API_1_PATH = "/test-1";
    private static final String API_2_PATH = "/test-2";
    private static final String PRODUCT_KEY = "cache-product-key";
    private static final String API_KEY = "cache-api-key";
    private static final String CACHE_NAME = "prod-cache";
    private static final String BACKEND = "/endpoint";

    abstract static class CachePreparer extends ApiProductV4FlowsIntegrationTest.FlowsTestPreparer {

        @Override
        public void configurePolicies(Map<String, PolicyPlugin> policies) {
            super.configurePolicies(policies);
            policies.put("cache", PolicyBuilder.build("cache", CachePolicy.class, CachePolicyConfiguration.class));
        }

        @Override
        public void configureResources(Map<String, ResourcePlugin> resources) {
            super.configureResources(resources);
            resources.put("cache", ResourceBuilder.build("cache", NodeCacheResource.class, CacheResourceConfiguration.class));
        }

        void deployCachingProduct(List<Flow> planFlows, List<Flow> productFlows, List<Resource> resources) {
            Plan plan = productPlan(PLAN_ID, "api-key", PlanStatus.PUBLISHED);
            plan.setFlows(planFlows);
            ReactableApiProduct product = product(PRODUCT_ID, Set.of(API_1_ID, API_2_ID));
            product.setPlans(List.of(plan));
            product.setFlows(productFlows);
            product.setResources(resources);
            deployApiProduct(product);
            allowProductKey(PRODUCT_ID, PLAN_ID, PRODUCT_KEY);
        }

        static List<Resource> cacheResource(String name, boolean enabled) {
            return List.of(
                Resource.builder()
                    .name(name)
                    .type("cache")
                    .enabled(enabled)
                    .configuration("{\"timeToLiveSeconds\":60,\"timeToIdleSeconds\":60,\"maxEntriesLocalHeap\":100}")
                    .build()
            );
        }

        static Step cacheStep(String cacheName) {
            return Step.builder()
                .name("cache")
                .enabled(true)
                .policy("cache")
                .configuration(
                    "{\"scope\":\"API\",\"cacheName\":\"" +
                        cacheName +
                        "\",\"methods\":[\"GET\"],\"timeToLiveSeconds\":60," +
                        "\"responseCondition\":\"{#upstreamResponse.status == 200}\"}"
                )
                .build();
        }

        static Flow cacheFlow(String cacheName) {
            return flow("cache", "/", List.of(cacheStep(cacheName)), List.of());
        }

        void stubBackend(String body) {
            wiremock.stubFor(get(urlPathEqualTo(BACKEND)).willReturn(ok(body)));
        }

        int backendHits() {
            return wiremock.findAll(getRequestedFor(urlPathEqualTo(BACKEND))).size();
        }

        CallResult exchange(HttpClient client, String path, String key) {
            return client
                .rxRequest(io.vertx.core.http.HttpMethod.GET, path)
                .flatMap(request -> request.putHeader("X-Gravitee-Api-Key", key).rxSend())
                .flatMap(response -> response.rxBody().map(body -> new CallResult(response.statusCode(), body.toString())))
                .blockingGet();
        }

        void assertSecondCallIsCached(HttpClient client, String path, String key, String body, int hitsAfterFirst) {
            Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    CallResult cached = exchange(client, path, key);
                    assertThat(cached.status()).isEqualTo(200);
                    assertThat(cached.body()).isEqualTo(body);
                    assertThat(backendHits()).isEqualTo(hitsAfterFirst);
                });
        }
    }

    @Nested
    @GatewayTest
    @DeployApi({ "/apis/v4/http/api-product/api-1.json", "/apis/v4/http/api-product/api-2.json" })
    class ProductPlanFlow extends CachePreparer {

        @Test
        void should_serve_each_members_second_call_from_the_product_cache(HttpClient client) {
            deployCachingProduct(List.of(cacheFlow(CACHE_NAME)), List.of(), cacheResource(CACHE_NAME, true));
            stubBackend("from-a");

            CallResult first = exchange(client, API_1_PATH, PRODUCT_KEY);
            assertThat(first.status()).isEqualTo(200);
            assertThat(first.body()).isEqualTo("from-a");
            int hitsAfterA = backendHits();
            assertThat(hitsAfterA).isEqualTo(1);
            stubBackend("from-a-changed");
            assertSecondCallIsCached(client, API_1_PATH, PRODUCT_KEY, "from-a", hitsAfterA);

            stubBackend("from-b");
            CallResult firstOnB = exchange(client, API_2_PATH, PRODUCT_KEY);
            assertThat(firstOnB.status()).isEqualTo(200);
            assertThat(firstOnB.body()).isEqualTo("from-b");
            int hitsAfterB = backendHits();
            assertThat(hitsAfterB).isEqualTo(hitsAfterA + 1);
            stubBackend("from-b-changed");
            assertSecondCallIsCached(client, API_2_PATH, PRODUCT_KEY, "from-b", hitsAfterB);
        }
    }

    @Nested
    @GatewayTest
    @DeployApi({ "/apis/v4/http/api-product/api-1.json", "/apis/v4/http/api-product/api-2.json" })
    class ProductFlow extends CachePreparer {

        @Test
        void should_serve_each_members_second_call_from_the_product_cache_when_the_policy_is_on_a_product_flow(HttpClient client) {
            deployCachingProduct(List.of(), List.of(cacheFlow(CACHE_NAME)), cacheResource(CACHE_NAME, true));
            stubBackend("from-a");

            CallResult first = exchange(client, API_1_PATH, PRODUCT_KEY);
            assertThat(first.status()).isEqualTo(200);
            assertThat(first.body()).isEqualTo("from-a");
            assertThat(backendHits()).isEqualTo(1);
            stubBackend("from-a-changed");
            assertSecondCallIsCached(client, API_1_PATH, PRODUCT_KEY, "from-a", 1);

            stubBackend("from-b");
            CallResult firstOnB = exchange(client, API_2_PATH, PRODUCT_KEY);
            assertThat(firstOnB.body()).isEqualTo("from-b");
            assertThat(backendHits()).isEqualTo(2);
            stubBackend("from-b-changed");
            assertSecondCallIsCached(client, API_2_PATH, PRODUCT_KEY, "from-b", 2);
        }
    }

    @Nested
    @GatewayTest
    @DeployApi({ "/apis/v4/http/api-product/api-1.json", "/apis/v4/http/api-product/api-2.json" })
    class SameNameOnTheMemberApi extends CachePreparer {

        @Override
        public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
            super.configureApi(api, definitionClass);
            if (API_1_ID.equals(api.getId())) {
                Api definition = (Api) api.getDefinition();
                definition.setResources(cacheResource(CACHE_NAME, true));
                definition
                    .getPlans()
                    .stream()
                    .filter(plan -> PLAN_APIKEY_ID.equals(plan.getId()))
                    .findFirst()
                    .orElseThrow()
                    .setFlows(List.of(cacheFlow(CACHE_NAME)));
            }
        }

        @Test
        void should_keep_the_product_cache_and_the_member_api_cache_apart(HttpClient client) {
            deployCachingProduct(List.of(), List.of(cacheFlow(CACHE_NAME)), cacheResource(CACHE_NAME, true));
            allowKeyForApi(API_KEY, API_1_ID);
            stubBackend("product-body");

            CallResult productCall = exchange(client, API_1_PATH, PRODUCT_KEY);
            assertThat(productCall.status()).isEqualTo(200);
            assertThat(productCall.body()).isEqualTo("product-body");
            assertSecondCallIsCached(client, API_1_PATH, PRODUCT_KEY, "product-body", 1);

            stubBackend("api-body");
            CallResult direct = exchange(client, API_1_PATH, API_KEY);
            assertThat(direct.status()).isEqualTo(200);
            assertThat(direct.body()).isEqualTo("api-body");
            assertThat(backendHits()).isEqualTo(2);
            stubBackend("api-body-changed");
            assertSecondCallIsCached(client, API_1_PATH, API_KEY, "api-body", 2);

            stubBackend("product-body-changed");
            assertSecondCallIsCached(client, API_1_PATH, PRODUCT_KEY, "product-body", 2);
        }
    }

    @Nested
    @GatewayTest
    @DeployApi({ "/apis/v4/http/api-product/api-1.json", "/apis/v4/http/api-product/api-2.json" })
    class ApiFlowOnAProductSubscription extends CachePreparer {

        @Override
        public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
            super.configureApi(api, definitionClass);
            if (API_1_ID.equals(api.getId())) {
                Api definition = (Api) api.getDefinition();
                definition.setResources(cacheResource(CACHE_NAME, true));
                definition.setFlows(List.of(cacheFlow(CACHE_NAME)));
            }
        }

        @Test
        void should_use_the_member_api_cache_when_the_policy_is_on_an_api_flow(HttpClient client) {
            deployCachingProduct(List.of(), List.of(), cacheResource(CACHE_NAME, true));
            allowKeyForApi(API_KEY, API_1_ID);
            stubBackend("api-flow-body");

            CallResult throughProduct = exchange(client, API_1_PATH, PRODUCT_KEY);
            assertThat(throughProduct.status()).isEqualTo(200);
            assertThat(throughProduct.body()).isEqualTo("api-flow-body");
            assertThat(backendHits()).isEqualTo(1);

            stubBackend("api-flow-body-changed");
            assertSecondCallIsCached(client, API_1_PATH, API_KEY, "api-flow-body", 1);
        }
    }

    @Nested
    @GatewayTest
    @DeployApi({ "/apis/v4/http/api-product/api-1.json", "/apis/v4/http/api-product/api-2.json" })
    class MissingProductResource extends CachePreparer {

        @Override
        public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
            super.configureApi(api, definitionClass);
            if (API_1_ID.equals(api.getId())) {
                Api definition = (Api) api.getDefinition();
                definition.setResources(cacheResource(CACHE_NAME, true));
                definition.setFlows(List.of(cacheFlow(CACHE_NAME)));
            }
        }

        @Test
        void should_fail_like_a_missing_api_cache_resource_and_not_read_the_member_cache(HttpClient client) {
            allowKeyForApi(API_KEY, API_1_ID);

            deployCachingProduct(List.of(cacheFlow(CACHE_NAME)), List.of(), List.of());
            stubBackend("should-not-be-cached-from-api");
            int hitsBefore = backendHits();

            CallResult missing = exchange(client, API_1_PATH, PRODUCT_KEY);

            assertThat(missing.status()).isEqualTo(500);
            assertThat(missing.body()).contains("No cache has been defined with name " + CACHE_NAME);
            assertThat(backendHits()).isEqualTo(hitsBefore);

            wiremock.resetRequests();
            stubBackend("direct-body");
            CallResult direct = exchange(client, API_1_PATH, API_KEY);
            assertThat(direct.status()).isEqualTo(200);
            assertThat(direct.body()).isEqualTo("direct-body");
            assertThat(backendHits()).isEqualTo(1);
        }

        @Test
        void should_fail_when_the_product_cache_resource_is_disabled_and_not_read_the_member_cache(HttpClient client) {
            deployCachingProduct(List.of(cacheFlow(CACHE_NAME)), List.of(), cacheResource(CACHE_NAME, false));
            stubBackend("should-not-be-served");
            int hitsBefore = backendHits();

            CallResult disabled = exchange(client, API_1_PATH, PRODUCT_KEY);

            assertThat(disabled.status()).isEqualTo(500);
            assertThat(disabled.body()).contains("No cache has been defined with name " + CACHE_NAME);
            assertThat(backendHits()).isEqualTo(hitsBefore);
        }
    }

    record CallResult(int status, String body) {}
}
