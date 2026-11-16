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
package io.gravitee.gateway.handlers.api.manager;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiProductChangeTest {

    private static final Date DEPLOYED_AT = new Date(1_000);
    private static final Date REDEPLOYED_AT = new Date(2_000);
    private static final Predicate<Plan> SERVED_ON_EU_GATEWAY = plan -> plan.getTags() != null && plan.getTags().contains("eu");

    @Nested
    class FirstDeployment {

        @Test
        void should_load_every_plan_and_resync_every_api() {
            ApiProductChange change = between(null, product(Set.of("api-1", "api-2"), plan("plan-1"), plan("plan-2")));

            assertThat(change.planIdsToLoadSubscriptionsFor()).containsExactlyInAnyOrder("plan-1", "plan-2");
            assertThat(change.planIdsToEvict()).isEmpty();
            assertThat(change.apiIdsToResync()).containsExactlyInAnyOrder("api-1", "api-2");
            assertThat(change.affectsMemberApis()).isTrue();
        }

        @Test
        void should_load_no_plan_when_the_product_has_no_api() {
            ApiProductChange change = between(null, product(Set.of(), plan("plan-1")));

            assertThat(change.planIdsToLoadSubscriptionsFor()).isEmpty();
        }
    }

    @Nested
    class Redeployment {

        @Test
        void should_change_nothing_when_only_deployed_at_differs() {
            ApiProductChange change = between(
                product(Set.of("api-1"), plan("plan-1")),
                redeployed(product(Set.of("api-1"), plan("plan-1")))
            );

            assertThat(change.planIdsToLoadSubscriptionsFor()).isEmpty();
            assertThat(change.planIdsToEvict()).isEmpty();
            assertThat(change.apiIdsToResync()).isEmpty();
            assertThat(change.affectsMemberApis()).isFalse();
        }

        @Test
        void should_only_touch_the_index_when_name_description_version_analytics_or_tags_change() {
            ReactableApiProduct incoming = redeployed(product(Set.of("api-1"), plan("plan-1")));
            incoming.setName("Renamed");
            incoming.setDescription("New description");
            incoming.setVersion("2.0");
            incoming.setAnalytics(new Analytics());
            incoming.setTags(Set.of("internal"));

            ApiProductChange change = between(product(Set.of("api-1"), plan("plan-1")), incoming);

            assertThat(change.planIdsToLoadSubscriptionsFor()).isEmpty();
            assertThat(change.planIdsToEvict()).isEmpty();
            assertThat(change.apiIdsToResync()).isEmpty();
            assertThat(change.affectsMemberApis()).isFalse();
        }

        @Test
        void should_load_only_the_added_plan() {
            ApiProductChange change = between(
                product(Set.of("api-1"), plan("plan-1")),
                redeployed(product(Set.of("api-1"), plan("plan-1"), plan("plan-2")))
            );

            assertThat(change.planIdsToLoadSubscriptionsFor()).containsExactly("plan-2");
            assertThat(change.planIdsToEvict()).isEmpty();
            assertThat(change.apiIdsToResync()).isEmpty();
            assertThat(change.affectsMemberApis()).isTrue();
        }

        @Test
        void should_evict_the_removed_plan() {
            ApiProductChange change = between(
                product(Set.of("api-1"), plan("plan-1"), plan("plan-2")),
                redeployed(product(Set.of("api-1"), plan("plan-1")))
            );

            assertThat(change.planIdsToEvict()).containsExactly("plan-2");
            assertThat(change.planIdsToLoadSubscriptionsFor()).isEmpty();
            assertThat(change.apiIdsToResync()).isEmpty();
            assertThat(change.affectsMemberApis()).isTrue();
        }

        @Test
        void should_reload_nothing_when_only_a_plan_flow_changes() {
            Plan withFlow = plan("plan-1");
            withFlow.setFlows(List.of(Flow.builder().name("added flow").build()));

            ApiProductChange change = between(product(Set.of("api-1"), plan("plan-1")), redeployed(product(Set.of("api-1"), withFlow)));

            assertThat(change.planIdsToLoadSubscriptionsFor()).isEmpty();
            assertThat(change.planIdsToEvict()).isEmpty();
            assertThat(change.apiIdsToResync()).isEmpty();
            assertThat(change.affectsMemberApis()).isTrue();
        }

        @Test
        void should_reload_the_subscriptions_of_a_kept_plan_whose_definition_changes() {
            Plan deprecated = plan("plan-1");
            deprecated.setStatus(PlanStatus.DEPRECATED);

            ApiProductChange change = between(
                product(Set.of("api-1"), plan("plan-1"), plan("plan-2")),
                redeployed(product(Set.of("api-1"), deprecated, plan("plan-2")))
            );

            assertThat(change.planIdsToLoadSubscriptionsFor()).containsExactly("plan-1");
            assertThat(change.planIdsToEvict()).isEmpty();
            assertThat(change.affectsMemberApis()).isTrue();
        }

        @Test
        void should_notify_member_apis_when_plans_are_reordered() {
            ApiProductChange change = between(
                product(Set.of("api-1"), plan("plan-1"), plan("plan-2")),
                redeployed(product(Set.of("api-1"), plan("plan-2"), plan("plan-1")))
            );

            assertThat(change.planIdsToLoadSubscriptionsFor()).isEmpty();
            assertThat(change.affectsMemberApis()).isTrue();
        }

        @Test
        void should_resync_only_the_added_and_removed_apis() {
            ApiProductChange change = between(
                product(Set.of("api-1", "api-2"), plan("plan-1")),
                redeployed(product(Set.of("api-2", "api-3"), plan("plan-1")))
            );

            assertThat(change.apiIdsToResync()).containsExactlyInAnyOrder("api-1", "api-3");
            assertThat(change.planIdsToLoadSubscriptionsFor()).isEmpty();
            assertThat(change.planIdsToEvict()).isEmpty();
            assertThat(change.affectsMemberApis()).isTrue();
        }

        @Test
        void should_load_every_plan_when_the_first_api_joins_the_product() {
            ApiProductChange change = between(
                product(Set.of(), plan("plan-1"), plan("plan-2")),
                redeployed(product(Set.of("api-1"), plan("plan-1"), plan("plan-2")))
            );

            assertThat(change.planIdsToLoadSubscriptionsFor()).containsExactlyInAnyOrder("plan-1", "plan-2");
            assertThat(change.apiIdsToResync()).containsExactly("api-1");
        }

        @Test
        void should_resync_every_api_when_the_product_gets_its_first_plan() {
            ApiProductChange change = between(
                product(Set.of("api-1", "api-2")),
                redeployed(product(Set.of("api-1", "api-2"), plan("plan-1")))
            );

            assertThat(change.apiIdsToResync()).containsExactlyInAnyOrder("api-1", "api-2");
            assertThat(change.planIdsToLoadSubscriptionsFor()).containsExactly("plan-1");
        }

        @Test
        void should_resync_every_api_when_the_product_loses_its_last_plan() {
            ApiProductChange change = between(
                product(Set.of("api-1", "api-2"), plan("plan-1")),
                redeployed(product(Set.of("api-1", "api-2")))
            );

            assertThat(change.apiIdsToResync()).containsExactlyInAnyOrder("api-1", "api-2");
            assertThat(change.planIdsToEvict()).containsExactly("plan-1");
        }

        @Test
        void should_resync_every_api_when_the_only_plan_is_retagged_away_from_this_gateway() {
            ApiProductChange change = ApiProductChange.between(
                product(Set.of("api-1", "api-2"), tagged(plan("plan-1"), "eu")),
                redeployed(product(Set.of("api-1", "api-2"), tagged(plan("plan-1"), "us"))),
                SERVED_ON_EU_GATEWAY
            );

            assertThat(change.apiIdsToResync()).containsExactlyInAnyOrder("api-1", "api-2");
        }

        @Test
        void should_resync_every_api_when_the_only_plan_is_retagged_onto_this_gateway() {
            ApiProductChange change = ApiProductChange.between(
                product(Set.of("api-1", "api-2"), tagged(plan("plan-1"), "us")),
                redeployed(product(Set.of("api-1", "api-2"), tagged(plan("plan-1"), "eu"))),
                SERVED_ON_EU_GATEWAY
            );

            assertThat(change.apiIdsToResync()).containsExactlyInAnyOrder("api-1", "api-2");
        }

        @Test
        void should_not_resync_when_a_plan_not_served_here_is_added_next_to_one_that_is() {
            ApiProductChange change = ApiProductChange.between(
                product(Set.of("api-1"), tagged(plan("plan-1"), "eu")),
                redeployed(product(Set.of("api-1"), tagged(plan("plan-1"), "eu"), tagged(plan("plan-2"), "us"))),
                SERVED_ON_EU_GATEWAY
            );

            assertThat(change.apiIdsToResync()).isEmpty();
        }

        @Test
        void should_list_the_member_apis_of_both_versions() {
            ApiProductChange change = between(
                product(Set.of("api-1", "api-2"), plan("plan-1")),
                redeployed(product(Set.of("api-2", "api-3"), plan("plan-1")))
            );

            assertThat(change.memberApiIds()).containsExactlyInAnyOrder("api-1", "api-2", "api-3");
        }

        @Test
        void should_treat_null_plans_and_api_ids_as_empty() {
            ReactableApiProduct deployed = product(Set.of());
            deployed.setApiIds(null);
            deployed.setPlans(null);
            ReactableApiProduct incoming = redeployed(product(Set.of()));
            incoming.setApiIds(null);
            incoming.setPlans(null);

            ApiProductChange change = between(deployed, incoming);

            assertThat(change.planIdsToLoadSubscriptionsFor()).isEmpty();
            assertThat(change.planIdsToEvict()).isEmpty();
            assertThat(change.apiIdsToResync()).isEmpty();
            assertThat(change.affectsMemberApis()).isFalse();
        }

        @Test
        void should_affect_member_apis_when_a_resource_configuration_changes() {
            Resource deployedResource = Resource.builder().name("cache").type("cache").configuration("{\"ttl\":10}").enabled(true).build();
            Resource incomingResource = Resource.builder().name("cache").type("cache").configuration("{\"ttl\":20}").enabled(true).build();
            ReactableApiProduct deployed = product(Set.of("api-1"), plan("plan-1"));
            deployed.setResources(List.of(deployedResource));
            ReactableApiProduct incoming = redeployed(product(Set.of("api-1"), plan("plan-1")));
            incoming.setResources(List.of(incomingResource));

            ApiProductChange change = between(deployed, incoming);

            assertThat(change.planIdsToLoadSubscriptionsFor()).isEmpty();
            assertThat(change.planIdsToEvict()).isEmpty();
            assertThat(change.apiIdsToResync()).isEmpty();
            assertThat(change.affectsMemberApis()).isTrue();
        }
    }

    @Nested
    class ServedOnThisGateway {

        @Test
        void should_load_only_the_plans_served_here_when_the_first_api_joins() {
            ApiProductChange change = ApiProductChange.between(
                product(Set.of(), tagged(plan("plan-eu"), "eu"), tagged(plan("plan-us"), "us")),
                redeployed(product(Set.of("api-1"), tagged(plan("plan-eu"), "eu"), tagged(plan("plan-us"), "us"))),
                SERVED_ON_EU_GATEWAY
            );

            assertThat(change.planIdsToLoadSubscriptionsFor()).containsExactly("plan-eu");
        }

        @Test
        void should_not_load_an_added_plan_not_served_here() {
            ApiProductChange change = ApiProductChange.between(
                product(Set.of("api-1"), tagged(plan("plan-eu"), "eu")),
                redeployed(product(Set.of("api-1"), tagged(plan("plan-eu"), "eu"), tagged(plan("plan-us"), "us"))),
                SERVED_ON_EU_GATEWAY
            );

            assertThat(change.planIdsToLoadSubscriptionsFor()).isEmpty();
        }

        @Test
        void should_evict_and_not_load_a_kept_plan_retagged_away_from_this_gateway() {
            ApiProductChange change = ApiProductChange.between(
                product(Set.of("api-1"), tagged(plan("plan-1"), "eu")),
                redeployed(product(Set.of("api-1"), tagged(plan("plan-1"), "us"))),
                SERVED_ON_EU_GATEWAY
            );

            assertThat(change.planIdsToLoadSubscriptionsFor()).isEmpty();
            assertThat(change.planIdsToEvict()).containsExactly("plan-1");
        }

        @Test
        void should_load_a_kept_plan_retagged_onto_this_gateway() {
            ApiProductChange change = ApiProductChange.between(
                product(Set.of("api-1"), tagged(plan("plan-1"), "us")),
                redeployed(product(Set.of("api-1"), tagged(plan("plan-1"), "eu"))),
                SERVED_ON_EU_GATEWAY
            );

            assertThat(change.planIdsToLoadSubscriptionsFor()).containsExactly("plan-1");
            assertThat(change.planIdsToEvict()).isEmpty();
        }

        @Test
        void should_not_evict_a_kept_plan_never_served_here() {
            ApiProductChange change = ApiProductChange.between(
                product(Set.of("api-1"), tagged(plan("plan-1"), "us")),
                redeployed(product(Set.of("api-1"), tagged(plan("plan-1"), "asia"))),
                SERVED_ON_EU_GATEWAY
            );

            assertThat(change.planIdsToEvict()).isEmpty();
        }
    }

    @Nested
    class FailedLoad {

        @Test
        void should_evict_an_added_plan_when_its_load_fails() {
            ApiProductChange change = between(
                product(Set.of("api-1"), plan("plan-1")),
                redeployed(product(Set.of("api-1"), plan("plan-1"), plan("plan-2")))
            );

            assertThat(change.planIdsToEvictIfLoadFails()).containsExactly("plan-2");
        }

        @Test
        void should_keep_a_reloaded_plan_the_deployed_version_already_caches() {
            Plan deprecated = plan("plan-1");
            deprecated.setStatus(PlanStatus.DEPRECATED);

            ApiProductChange change = between(product(Set.of("api-1"), plan("plan-1")), redeployed(product(Set.of("api-1"), deprecated)));

            assertThat(change.planIdsToLoadSubscriptionsFor()).containsExactly("plan-1");
            assertThat(change.planIdsToEvictIfLoadFails()).isEmpty();
        }

        @Test
        void should_evict_every_loaded_plan_when_the_first_api_joins() {
            ApiProductChange change = between(
                product(Set.of(), plan("plan-1"), plan("plan-2")),
                redeployed(product(Set.of("api-1"), plan("plan-1"), plan("plan-2")))
            );

            assertThat(change.planIdsToEvictIfLoadFails()).containsExactlyInAnyOrder("plan-1", "plan-2");
        }

        @Test
        void should_evict_a_plan_retagged_onto_this_gateway() {
            ApiProductChange change = ApiProductChange.between(
                product(Set.of("api-1"), tagged(plan("plan-1"), "us")),
                redeployed(product(Set.of("api-1"), tagged(plan("plan-1"), "eu"))),
                SERVED_ON_EU_GATEWAY
            );

            assertThat(change.planIdsToEvictIfLoadFails()).containsExactly("plan-1");
        }
    }

    @Nested
    class MemberApis {

        @Test
        void should_count_a_product_field_not_known_to_be_index_only_as_a_change() {
            ReactableApiProduct incoming = redeployed(product(Set.of("api-1"), plan("plan-1")));
            incoming.setEnvironmentHrid("renamed-env");

            ApiProductChange change = between(product(Set.of("api-1"), plan("plan-1")), incoming);

            assertThat(change.affectsMemberApis()).isTrue();
        }
    }

    private static ApiProductChange between(ReactableApiProduct deployed, ReactableApiProduct incoming) {
        return ApiProductChange.between(deployed, incoming, plan -> true);
    }

    private static Plan tagged(Plan plan, String tag) {
        plan.setTags(Set.of(tag));
        return plan;
    }

    private static ReactableApiProduct product(Set<String> apiIds, Plan... plans) {
        return ReactableApiProduct.builder()
            .id("product-1")
            .name("Product")
            .description("Description")
            .version("1.0")
            .apiIds(apiIds)
            .environmentId("env-1")
            .deployedAt(DEPLOYED_AT)
            .plans(new ArrayList<>(List.of(plans)))
            .build();
    }

    private static ReactableApiProduct redeployed(ReactableApiProduct product) {
        product.setDeployedAt(REDEPLOYED_AT);
        return product;
    }

    private static Plan plan(String id) {
        return Plan.builder()
            .id(id)
            .name("Plan " + id)
            .security(PlanSecurity.builder().type("api-key").build())
            .mode(PlanMode.STANDARD)
            .status(PlanStatus.PUBLISHED)
            .build();
    }
}
