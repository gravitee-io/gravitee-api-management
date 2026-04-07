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
package io.gravitee.apim.core.plan.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.PlanFixtures;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.query_service.PlanSearchQueryService;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.apim.core.subscription.query_service.SubscriptionQueryService;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
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
class GetPlansUseCaseTest {

    private static final String API_PRODUCT_ID = "c45b8e66-4d2a-47ad-9b8e-664d2a97ad88";
    private static final String PLAN_ID = "plan-id";
    private static final String USER = "user";
    private static final String APPLICATION_ID = "application-id";

    @Mock
    private PlanSearchQueryService planSearchQueryService;

    @Mock
    private SubscriptionQueryService subscriptionQueryService;

    private GetPlansUseCase getPlansUseCase;

    @BeforeEach
    void setUp() {
        getPlansUseCase = new GetPlansUseCase(planSearchQueryService, subscriptionQueryService);
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

            when(
                planSearchQueryService.searchPlans(
                    eq(API_PRODUCT_ID),
                    eq(GenericPlanEntity.ReferenceType.API_PRODUCT),
                    eq(query),
                    eq(USER),
                    eq(true)
                )
            ).thenReturn(List.of(planOrder2, planOrder1, planOrder3));

            var input = GetPlansUseCase.Input.of(API_PRODUCT_ID, GenericPlanEntity.ReferenceType.API_PRODUCT, USER, true, query, null);
            var output = getPlansUseCase.execute(input);

            assertThat(output.plans()).hasSize(3);
            assertThat(output.plan()).isEmpty();
            assertThat(output.plans()).extracting(Plan::getOrder).containsExactly(1, 2, 3);
            assertThat(output.plans()).extracting(Plan::getId).containsExactly("plan-1", "plan-2", "plan-3");

            verify(planSearchQueryService).searchPlans(
                eq(API_PRODUCT_ID),
                eq(GenericPlanEntity.ReferenceType.API_PRODUCT),
                eq(query),
                eq(USER),
                eq(true)
            );
        }

        @Test
        void should_return_empty_list_when_no_plans() {
            PlanQuery query = PlanQuery.builder().referenceId(API_PRODUCT_ID).build();

            when(planSearchQueryService.searchPlans(anyString(), any(), any(), anyString(), anyBoolean())).thenReturn(List.of());

            var input = GetPlansUseCase.Input.of(API_PRODUCT_ID, GenericPlanEntity.ReferenceType.API_PRODUCT, USER, false, query, null);
            var output = getPlansUseCase.execute(input);

            assertThat(output.plans()).isEmpty();
            assertThat(output.plan()).isEmpty();
        }

        @Test
        void should_pass_authenticated_user_and_is_admin_to_search() {
            PlanQuery query = PlanQuery.builder().referenceId(API_PRODUCT_ID).build();

            when(planSearchQueryService.searchPlans(anyString(), any(), any(), anyString(), anyBoolean())).thenReturn(List.of());

            var input = new GetPlansUseCase.Input(
                API_PRODUCT_ID,
                "custom-user",
                false,
                query,
                null,
                null,
                GenericPlanEntity.ReferenceType.API_PRODUCT
            );
            getPlansUseCase.execute(input);

            verify(planSearchQueryService).searchPlans(
                eq(API_PRODUCT_ID),
                eq(GenericPlanEntity.ReferenceType.API_PRODUCT),
                eq(query),
                eq("custom-user"),
                eq(false)
            );
        }

        @Test
        void should_filter_out_subscribed_plans_when_subscribableBy_provided() {
            Plan subscribedPlan = PlanFixtures.HttpV4.anApiKey().toBuilder().id("subscribed-plan").order(1).build();
            Plan availablePlan = PlanFixtures.HttpV4.anApiKey().toBuilder().id("available-plan").order(2).build();

            PlanQuery query = PlanQuery.builder().referenceId(API_PRODUCT_ID).build();

            when(
                planSearchQueryService.searchPlans(
                    eq(API_PRODUCT_ID),
                    eq(GenericPlanEntity.ReferenceType.API_PRODUCT),
                    eq(query),
                    eq(USER),
                    eq(true)
                )
            ).thenReturn(List.of(subscribedPlan, availablePlan));

            SubscriptionEntity subscription = SubscriptionEntity.builder().planId("subscribed-plan").build();
            when(
                subscriptionQueryService.findActiveByApplicationIdAndReferenceIdAndReferenceType(
                    eq(APPLICATION_ID),
                    eq(API_PRODUCT_ID),
                    eq(SubscriptionReferenceType.API_PRODUCT)
                )
            ).thenReturn(List.of(subscription));

            var input = GetPlansUseCase.Input.of(
                API_PRODUCT_ID,
                GenericPlanEntity.ReferenceType.API_PRODUCT,
                USER,
                true,
                query,
                APPLICATION_ID
            );
            var output = getPlansUseCase.execute(input);

            assertThat(output.plans()).hasSize(1);
            assertThat(output.plans()).extracting(Plan::getId).containsExactly("available-plan");
        }

        @Test
        void should_return_all_plans_when_no_subscriptions_exist() {
            Plan plan1 = PlanFixtures.HttpV4.anApiKey().toBuilder().id("plan-1").order(1).build();
            Plan plan2 = PlanFixtures.HttpV4.anApiKey().toBuilder().id("plan-2").order(2).build();

            PlanQuery query = PlanQuery.builder().referenceId(API_PRODUCT_ID).build();

            when(
                planSearchQueryService.searchPlans(
                    eq(API_PRODUCT_ID),
                    eq(GenericPlanEntity.ReferenceType.API_PRODUCT),
                    eq(query),
                    eq(USER),
                    eq(true)
                )
            ).thenReturn(List.of(plan1, plan2));

            when(
                subscriptionQueryService.findActiveByApplicationIdAndReferenceIdAndReferenceType(
                    eq(APPLICATION_ID),
                    eq(API_PRODUCT_ID),
                    eq(SubscriptionReferenceType.API_PRODUCT)
                )
            ).thenReturn(List.of());

            var input = GetPlansUseCase.Input.of(
                API_PRODUCT_ID,
                GenericPlanEntity.ReferenceType.API_PRODUCT,
                USER,
                true,
                query,
                APPLICATION_ID
            );
            var output = getPlansUseCase.execute(input);

            assertThat(output.plans()).hasSize(2);
            assertThat(output.plans()).extracting(Plan::getId).containsExactly("plan-1", "plan-2");
        }

        @Test
        void should_not_filter_when_subscribableBy_is_null() {
            Plan plan1 = PlanFixtures.HttpV4.anApiKey().toBuilder().id("plan-1").order(1).build();
            Plan plan2 = PlanFixtures.HttpV4.anApiKey().toBuilder().id("plan-2").order(2).build();

            PlanQuery query = PlanQuery.builder().referenceId(API_PRODUCT_ID).build();

            when(
                planSearchQueryService.searchPlans(
                    eq(API_PRODUCT_ID),
                    eq(GenericPlanEntity.ReferenceType.API_PRODUCT),
                    eq(query),
                    eq(USER),
                    eq(true)
                )
            ).thenReturn(List.of(plan1, plan2));

            var input = GetPlansUseCase.Input.of(API_PRODUCT_ID, GenericPlanEntity.ReferenceType.API_PRODUCT, USER, true, query, null);
            var output = getPlansUseCase.execute(input);

            assertThat(output.plans()).hasSize(2);
            verify(subscriptionQueryService, never()).findActiveByApplicationIdAndReferenceIdAndReferenceType(any(), any(), any());
        }
    }

    @Nested
    class GetSinglePlan {

        @Test
        void should_return_single_plan_by_id() {
            Plan plan = Plan.builder().id(PLAN_ID).name("My Plan").build();

            when(
                planSearchQueryService.findByPlanIdAndReferenceIdAndReferenceType(
                    eq(PLAN_ID),
                    eq(API_PRODUCT_ID),
                    eq(GenericPlanEntity.ReferenceType.API_PRODUCT)
                )
            ).thenReturn(plan);

            var input = GetPlansUseCase.Input.of(API_PRODUCT_ID, PLAN_ID, GenericPlanEntity.ReferenceType.API_PRODUCT);
            var output = getPlansUseCase.execute(input);

            assertThat(output.plans()).isNull();
            assertThat(output.plan()).isPresent();
            assertThat(output.plan().get().getId()).isEqualTo(PLAN_ID);
            assertThat(output.plan().get().getName()).isEqualTo("My Plan");

            verify(planSearchQueryService).findByPlanIdAndReferenceIdAndReferenceType(
                eq(PLAN_ID),
                eq(API_PRODUCT_ID),
                eq(GenericPlanEntity.ReferenceType.API_PRODUCT)
            );
        }

        @Test
        void should_use_input_of_factory_for_single_plan() {
            Plan plan = Plan.builder().id(PLAN_ID).build();

            when(
                planSearchQueryService.findByPlanIdAndReferenceIdAndReferenceType(
                    eq(PLAN_ID),
                    eq(API_PRODUCT_ID),
                    eq(GenericPlanEntity.ReferenceType.API_PRODUCT)
                )
            ).thenReturn(plan);

            var input = GetPlansUseCase.Input.of(API_PRODUCT_ID, PLAN_ID, GenericPlanEntity.ReferenceType.API_PRODUCT);
            getPlansUseCase.execute(input);

            verify(planSearchQueryService).findByPlanIdAndReferenceIdAndReferenceType(
                eq(PLAN_ID),
                eq(API_PRODUCT_ID),
                eq(GenericPlanEntity.ReferenceType.API_PRODUCT)
            );
        }
    }
}
