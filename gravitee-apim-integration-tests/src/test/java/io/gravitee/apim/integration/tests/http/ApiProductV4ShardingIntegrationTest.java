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

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.gravitee.gateway.handlers.api.registry.ApiProductRegistry;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * How API Product sharding tags decide what a gateway serves.
 *
 * <p>The gateway here runs with the {@code internal} tag. Products and product plans carry their
 * own tags, and the registry indexes a product plan for an API only when both match — which is what
 * makes a product subscription resolvable through that API.</p>
 *
 * <p>Asserted on the registry rather than through HTTP calls: what sharding decides is whether an
 * API resolves to a product at all, and the registry is where that decision is made and observable.
 * A request-level assertion would only show a 401 without telling which of the many reasons caused
 * it.</p>
 */
@GatewayTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@DeployApi({ "/apis/v4/http/api-product/api-1.json", "/apis/v4/http/api-product/api-2.json" })
class ApiProductV4ShardingIntegrationTest extends ApiProductV4IntegrationTest.TestPreparer {

    private static final String SHARDING_TAGS_PROPERTY = "tags";
    private static final String GATEWAY_TAG = "internal";
    private static final String OTHER_TAG = "external";
    private static final String ENV_ID = "DEFAULT";
    private static final String API_1_ID = "my-api-v4-1";
    private static final String PRODUCT_ID = "sharding-product";
    private static final String PLAN_ID = "sharding-product-plan";

    @Override
    protected void configureGateway(GatewayConfigurationBuilder builder) {
        builder.setSystemProperty(SHARDING_TAGS_PROPERTY, GATEWAY_TAG);
    }

    /**
     * The SDK writes gateway configuration through {@link System#setProperty} and never clears it,
     * so a sharding tag set here would otherwise outlive this class and make every later test run
     * on a tagged gateway — where untagged APIs are not deployed at all, and calls answer 404.
     */
    @AfterAll
    static void clearShardingTags() {
        System.clearProperty(SHARDING_TAGS_PROPERTY);
    }

    @Test
    void should_serve_a_product_whose_tags_match_the_gateway() {
        deployProduct(Set.of(GATEWAY_TAG), null);

        assertThat(registry().getApiProductIdsForApi(API_1_ID)).containsExactly(PRODUCT_ID);
        assertThat(registry().getApiProductPlanEntriesForApi(API_1_ID, ENV_ID)).hasSize(1);
    }

    @Test
    void should_ignore_a_product_whose_tags_do_not_match_the_gateway() {
        deployProduct(Set.of(OTHER_TAG), null);

        assertThat(registry().getApiProductIdsForApi(API_1_ID)).isEmpty();
        assertThat(registry().getApiProductPlanEntriesForApi(API_1_ID, ENV_ID)).isEmpty();
    }

    /**
     * Products and plans do not treat the absence of tags alike, and the difference is deliberate:
     * {@code ApiProductShardingFilter} short-circuits an untagged plan to accepted, while an
     * untagged product goes through the ordinary sharding rule — a tagged gateway serves only what
     * carries its tags, exactly as it does for APIs. An untagged product is therefore invisible
     * here, and would be served by an untagged gateway.
     */
    @Test
    void should_ignore_an_untagged_product_on_a_tagged_gateway() {
        deployProduct(null, null);

        assertThat(registry().getApiProductIdsForApi(API_1_ID)).isEmpty();
    }

    /**
     * The counterpart: an untagged plan rides on its product's tags.
     *
     * <p>This is where API Products part company with APIs. An untagged <em>API</em> plan goes
     * through the ordinary rule and can make a tagged gateway skip its API entirely — see
     * {@code ApiManagerImplTest.should_not_deploy_api_if_plan_has_non_matching_tag}. An untagged
     * <em>product</em> plan is accepted outright. The reasoning behind that short-circuit is
     * recorded on {@code ApiProductShardingFilter}; this test pins the behaviour down either way.</p>
     */
    @Test
    void should_serve_an_untagged_plan_of_a_matching_product() {
        deployProduct(Set.of(GATEWAY_TAG), null);

        assertThat(registry().getApiProductPlanEntriesForApi(API_1_ID, ENV_ID)).hasSize(1);
    }

    /**
     * A plan carries its own tags on top of the product's. A plan the gateway does not serve leaves
     * the API with nothing to subscribe through, so the product stops resolving for it entirely.
     */
    @Test
    void should_ignore_a_plan_whose_tags_do_not_match_the_gateway() {
        deployProduct(Set.of(GATEWAY_TAG), Set.of(OTHER_TAG));

        assertThat(registry().getApiProductPlanEntriesForApi(API_1_ID, ENV_ID)).isEmpty();
        assertThat(registry().getApiProductIdsForApi(API_1_ID)).isEmpty();
    }

    @Test
    void should_serve_a_plan_whose_tags_match_the_gateway() {
        deployProduct(Set.of(GATEWAY_TAG), Set.of(GATEWAY_TAG));

        assertThat(registry().getApiProductPlanEntriesForApi(API_1_ID, ENV_ID)).hasSize(1);
        assertThat(registry().getApiProductIdsForApi(API_1_ID)).containsExactly(PRODUCT_ID);
    }

    /**
     * The API's own tags play no part here: membership of a served product is what exposes it,
     * whatever the API itself is tagged with. Worth pinning down, since this is the eligibility
     * question left open on the PR.
     */
    @Test
    void should_not_let_the_api_own_tags_decide_product_resolution() {
        deployProduct(Set.of(GATEWAY_TAG), Set.of(GATEWAY_TAG));

        // The deployed APIs carry no tag of their own, yet resolve to the product
        assertThat(registry().getApiProductIdsForApi(API_1_ID)).containsExactly(PRODUCT_ID);
    }

    @Test
    void should_stop_serving_a_product_retagged_away_from_the_gateway() {
        deployProduct(Set.of(GATEWAY_TAG), null);
        assertThat(registry().getApiProductIdsForApi(API_1_ID)).containsExactly(PRODUCT_ID);

        redeployProduct(Set.of(OTHER_TAG), null);

        assertThat(registry().getApiProductIdsForApi(API_1_ID)).isEmpty();
    }

    private ApiProductRegistry registry() {
        return getBean(ApiProductRegistry.class);
    }

    private void deployProduct(Set<String> productTags, Set<String> planTags) {
        deployApiProduct(buildProduct(productTags, planTags));
    }

    private void redeployProduct(Set<String> productTags, Set<String> planTags) {
        redeployApiProduct(buildProduct(productTags, planTags));
    }

    private ReactableApiProduct buildProduct(Set<String> productTags, Set<String> planTags) {
        Plan plan = Plan.builder()
            .id(PLAN_ID)
            .name("ShardingPlan")
            .security(PlanSecurity.builder().type("api-key").build())
            .status(PlanStatus.PUBLISHED)
            .mode(PlanMode.STANDARD)
            .tags(planTags)
            .build();

        return ReactableApiProduct.builder()
            .id(PRODUCT_ID)
            .name("Sharding product")
            .version("1.0.0")
            .apiIds(Set.of(API_1_ID))
            .environmentId(ENV_ID)
            .tags(productTags)
            .plans(List.of(plan))
            .build();
    }
}
