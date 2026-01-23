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
package io.gravitee.apim.core.plan.use_case.api_product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.PlanFixtures;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.query_service.ApiProductPlanSearchQueryService;
import io.gravitee.rest.api.model.v4.plan.PlanQuery;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class GetApiProductPlansUseCaseTest {

    private static final String API_PRODUCT_ID = "api-product-id";
    private static final String PLAN_ID = "plan-id";
    private static final String USER = "user";

    @Mock
    private ApiProductPlanSearchQueryService apiProductPlanSearchQueryService;

    private GetApiProductPlansUseCase getApiProductPlansUseCase;

    @BeforeEach
    void setUp() {
        getApiProductPlansUseCase = new GetApiProductPlansUseCase(apiProductPlanSearchQueryService);
    }

    @Nested
    class GetMultiplePlans {

        @Test
        void should_return_plans_list_sorted_by_order() {
            // use fixtures so Plan has a V4 definition (avoid NPE on getPlanStatus())
            Plan planOrder2 = PlanFixtures.aPlanHttpV4().toBuilder().id("plan-2").order(2).build();
            Plan planOrder1 = PlanFixtures.aPlanHttpV4().toBuilder().id("plan-1").order(1).build();
            Plan planOrder3 = PlanFixtures.aPlanHttpV4().toBuilder().id("plan-3").order(3).build();

            PlanQuery query = PlanQuery.builder().referenceId(API_PRODUCT_ID).build();

            when(apiProductPlanSearchQueryService.searchForApiProductPlans(eq(API_PRODUCT_ID), eq(query), eq(USER), eq(true))).thenReturn(
                List.of(planOrder2, planOrder1, planOrder3)
            );

            var input = GetApiProductPlansUseCase.Input.of(API_PRODUCT_ID, USER, true, query);
            var output = getApiProductPlansUseCase.execute(input);

            assertThat(output.apiProductPlans()).hasSize(3);
            assertThat(output.apiProductPlan()).isEmpty();
            assertThat(output.apiProductPlans()).extracting(Plan::getOrder).containsExactly(1, 2, 3);
            assertThat(output.apiProductPlans()).extracting(Plan::getId).containsExactly("plan-1", "plan-2", "plan-3");

            verify(apiProductPlanSearchQueryService).searchForApiProductPlans(eq(API_PRODUCT_ID), eq(query), eq(USER), eq(true));
        }

        @Test
        void should_return_empty_list_when_no_plans() {
            PlanQuery query = PlanQuery.builder().referenceId(API_PRODUCT_ID).build();

            when(apiProductPlanSearchQueryService.searchForApiProductPlans(any(), any(), any(), any(Boolean.class))).thenReturn(List.of());

            var input = GetApiProductPlansUseCase.Input.of(API_PRODUCT_ID, USER, false, query);
            var output = getApiProductPlansUseCase.execute(input);

            assertThat(output.apiProductPlans()).isEmpty();
            assertThat(output.apiProductPlan()).isEmpty();
        }

        @Test
        void should_pass_authenticated_user_and_is_admin_to_search() {
            PlanQuery query = PlanQuery.builder().referenceId(API_PRODUCT_ID).build();

            when(apiProductPlanSearchQueryService.searchForApiProductPlans(any(), any(), any(), any(Boolean.class))).thenReturn(List.of());

            var input = new GetApiProductPlansUseCase.Input(API_PRODUCT_ID, "custom-user", false, query, null);
            getApiProductPlansUseCase.execute(input);

            verify(apiProductPlanSearchQueryService).searchForApiProductPlans(eq(API_PRODUCT_ID), eq(query), eq("custom-user"), eq(false));
        }
    }

    @Nested
    class GetSinglePlan {

        @Test
        void should_return_single_plan_by_id() {
            Plan plan = Plan.builder().id(PLAN_ID).name("My Plan").build();

            when(apiProductPlanSearchQueryService.findByPlanIdIdForApiProduct(eq(PLAN_ID), eq(API_PRODUCT_ID))).thenReturn(plan);

            var input = GetApiProductPlansUseCase.Input.of(API_PRODUCT_ID, PLAN_ID);
            var output = getApiProductPlansUseCase.execute(input);

            assertThat(output.apiProductPlans()).isNull();
            assertThat(output.apiProductPlan()).isPresent();
            assertThat(output.apiProductPlan().get().getId()).isEqualTo(PLAN_ID);
            assertThat(output.apiProductPlan().get().getName()).isEqualTo("My Plan");

            verify(apiProductPlanSearchQueryService).findByPlanIdIdForApiProduct(eq(PLAN_ID), eq(API_PRODUCT_ID));
        }

        @Test
        void should_use_input_of_factory_for_single_plan() {
            Plan plan = Plan.builder().id(PLAN_ID).build();

            when(apiProductPlanSearchQueryService.findByPlanIdIdForApiProduct(eq(PLAN_ID), eq(API_PRODUCT_ID))).thenReturn(plan);

            var input = GetApiProductPlansUseCase.Input.of(API_PRODUCT_ID, PLAN_ID);
            getApiProductPlansUseCase.execute(input);

            verify(apiProductPlanSearchQueryService).findByPlanIdIdForApiProduct(eq(PLAN_ID), eq(API_PRODUCT_ID));
        }
    }
}
