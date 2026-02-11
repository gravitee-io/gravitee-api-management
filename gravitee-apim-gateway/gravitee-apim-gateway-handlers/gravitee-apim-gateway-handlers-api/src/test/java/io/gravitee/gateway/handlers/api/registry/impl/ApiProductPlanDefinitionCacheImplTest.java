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
package io.gravitee.gateway.handlers.api.registry.impl;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiProductPlanDefinitionCacheImplTest {

    private ApiProductPlanDefinitionCacheImpl cut;

    @BeforeEach
    void setUp() {
        cut = new ApiProductPlanDefinitionCacheImpl();
    }

    @Nested
    class RegisterTest {

        @Test
        void should_register_plans_for_api_product() {
            List<Plan> plans = List.of(createPlan("plan-1"), createPlan("plan-2"));

            cut.register("api-product-1", plans);

            List<?> retrieved = cut.getByApiProductId("api-product-1");
            assertThat(retrieved).hasSize(2);
        }

        @Test
        void should_overwrite_existing_plans_on_register() {
            cut.register("api-product-1", List.of(createPlan("plan-1")));
            cut.register("api-product-1", List.of(createPlan("plan-2"), createPlan("plan-3")));

            List<?> retrieved = cut.getByApiProductId("api-product-1");
            assertThat(retrieved).hasSize(2);
        }

        @Test
        void should_not_register_when_api_product_id_null() {
            cut.register(null, List.of(createPlan("plan-1")));

            assertThat(cut.getByApiProductId(null)).isEmpty();
        }

        @Test
        void should_not_register_when_plans_null() {
            cut.register("api-product-1", null);

            assertThat(cut.getByApiProductId("api-product-1")).isEmpty();
        }

        @Test
        void should_not_register_when_plans_empty() {
            cut.register("api-product-1", List.of());

            assertThat(cut.getByApiProductId("api-product-1")).isEmpty();
        }
    }

    @Nested
    class UnregisterTest {

        @Test
        void should_unregister_plans() {
            cut.register("api-product-1", List.of(createPlan("plan-1")));

            cut.unregister("api-product-1");

            assertThat(cut.getByApiProductId("api-product-1")).isEmpty();
        }

        @Test
        void should_not_fail_when_unregistering_non_existent() {
            cut.unregister("non-existent");
        }

        @Test
        void should_not_fail_when_unregistering_null() {
            cut.unregister(null);
        }
    }

    @Nested
    class GetByApiProductIdTest {

        @Test
        void should_return_empty_list_when_api_product_id_null() {
            cut.register("api-product-1", List.of(createPlan("plan-1")));

            List<?> retrieved = cut.getByApiProductId(null);

            assertThat(retrieved).isEmpty();
        }

        @Test
        void should_return_empty_list_when_not_found() {
            List<?> retrieved = cut.getByApiProductId("non-existent");

            assertThat(retrieved).isEmpty();
        }

        @Test
        void should_return_registered_plans() {
            Plan plan = createPlan("plan-1");
            cut.register("api-product-1", List.of(plan));

            List<?> retrieved = cut.getByApiProductId("api-product-1");

            assertThat(retrieved).hasSize(1);
            assertThat(((Plan) retrieved.get(0)).getId()).isEqualTo("plan-1");
        }
    }

    private Plan createPlan(String id) {
        Plan plan = new Plan();
        plan.setId(id);
        plan.setName("Plan " + id);
        plan.setStatus(PlanStatus.PUBLISHED);
        plan.setMode(PlanMode.STANDARD);
        plan.setSecurity(PlanSecurity.builder().type("api-key").configuration("{}").build());
        return plan;
    }
}
