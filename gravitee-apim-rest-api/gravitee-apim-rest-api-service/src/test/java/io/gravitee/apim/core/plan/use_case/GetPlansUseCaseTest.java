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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fixtures.core.model.PlanFixtures;
import inmemory.PlanCrudServiceInMemory;
import inmemory.PlanQueryServiceInMemory;
import inmemory.SubscriptionCrudServiceInMemory;
import inmemory.SubscriptionQueryServiceInMemory;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.query_service.PlanSearchQueryService;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.apim.infra.query_service.plan.PlanSearchQueryServiceImpl;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class GetPlansUseCaseTest {

    private static final String REFERENCE_ID = "ref-id";
    private static final String REFERENCE_TYPE_API = GenericPlanEntity.ReferenceType.API.name();
    private static final String REFERENCE_TYPE_API_PRODUCT = GenericPlanEntity.ReferenceType.API_PRODUCT.name();
    private static final String PLAN_ID_1 = "plan-1";
    private static final String PLAN_ID_2 = "plan-2";

    private PlanSearchQueryService planSearchQueryService;
    private SubscriptionQueryServiceInMemory subscriptionQueryService;
    private GetPlansUseCase getPlansUseCase;

    @BeforeEach
    void setUp() {
        PlanCrudServiceInMemory planCrudService = new PlanCrudServiceInMemory();
        PlanQueryServiceInMemory planQueryService = new PlanQueryServiceInMemory(planCrudService);
        planSearchQueryService = new PlanSearchQueryServiceImpl(planQueryService, planCrudService);
        subscriptionQueryService = new SubscriptionQueryServiceInMemory(new SubscriptionCrudServiceInMemory());
        getPlansUseCase = new GetPlansUseCase(planSearchQueryService, subscriptionQueryService);
    }

    @Nested
    class ListPlans {

        @Test
        void should_return_multiple_plans_sorted_by_order() {
            Plan plan1 = PlanFixtures.aPlanHttpV4()
                .toBuilder()
                .id(PLAN_ID_1)
                .order(2)
                .referenceId(REFERENCE_ID)
                .referenceType(GenericPlanEntity.ReferenceType.API)
                .build();
            Plan plan2 = PlanFixtures.aPlanHttpV4()
                .toBuilder()
                .id(PLAN_ID_2)
                .order(1)
                .referenceId(REFERENCE_ID)
                .referenceType(GenericPlanEntity.ReferenceType.API)
                .build();
            planSearchQueryService = mock(PlanSearchQueryService.class);
            when(planSearchQueryService.searchPlans(eq(REFERENCE_ID), eq(REFERENCE_TYPE_API), any(), any(), anyBoolean())).thenReturn(
                List.of(plan1, plan2)
            );
            getPlansUseCase = new GetPlansUseCase(planSearchQueryService, subscriptionQueryService);

            var input = GetPlansUseCase.Input.forList(REFERENCE_ID, REFERENCE_TYPE_API, "user", true, null, null);
            var output = getPlansUseCase.execute(input);

            assertThat(output.plans()).isNotNull();
            assertThat(output.plan()).isEmpty();
            assertThat(output.plans()).hasSize(2);
            assertThat(output.plans()).extracting(Plan::getId).containsExactly(PLAN_ID_2, PLAN_ID_1);
        }

        @Test
        void should_return_multiple_plans_sorted_by_order_for_api_product() {
            Plan plan1 = PlanFixtures.aPlanHttpV4()
                .toBuilder()
                .id(PLAN_ID_1)
                .order(2)
                .referenceId(REFERENCE_ID)
                .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                .build();
            Plan plan2 = PlanFixtures.aPlanHttpV4()
                .toBuilder()
                .id(PLAN_ID_2)
                .order(1)
                .referenceId(REFERENCE_ID)
                .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                .build();
            planSearchQueryService = mock(PlanSearchQueryService.class);
            when(
                planSearchQueryService.searchPlans(eq(REFERENCE_ID), eq(REFERENCE_TYPE_API_PRODUCT), any(), any(), anyBoolean())
            ).thenReturn(List.of(plan1, plan2));
            getPlansUseCase = new GetPlansUseCase(planSearchQueryService, subscriptionQueryService);

            var input = GetPlansUseCase.Input.forList(REFERENCE_ID, REFERENCE_TYPE_API_PRODUCT, "user", true, null, null);
            var output = getPlansUseCase.execute(input);

            assertThat(output.plans()).isNotNull();
            assertThat(output.plan()).isEmpty();
            assertThat(output.plans()).hasSize(2);
            assertThat(output.plans()).extracting(Plan::getId).containsExactly(PLAN_ID_2, PLAN_ID_1);
            assertThat(output.plans()).extracting(Plan::getReferenceType).containsOnly(GenericPlanEntity.ReferenceType.API_PRODUCT);
        }

        @Test
        void should_return_empty_list_when_no_plans() {
            planSearchQueryService = mock(PlanSearchQueryService.class);
            when(
                planSearchQueryService.searchPlans(eq(REFERENCE_ID), eq(REFERENCE_TYPE_API_PRODUCT), any(), any(), anyBoolean())
            ).thenReturn(List.of());
            getPlansUseCase = new GetPlansUseCase(planSearchQueryService, subscriptionQueryService);

            var input = GetPlansUseCase.Input.forList(REFERENCE_ID, REFERENCE_TYPE_API_PRODUCT, "user", false, null, null);
            var output = getPlansUseCase.execute(input);

            assertThat(output.plans()).isEmpty();
            assertThat(output.plan()).isEmpty();
        }

        @Test
        void should_filter_plans_when_subscribableBy_provided() {
            Plan plan1 = PlanFixtures.aPlanHttpV4()
                .toBuilder()
                .id(PLAN_ID_1)
                .order(1)
                .referenceId(REFERENCE_ID)
                .referenceType(GenericPlanEntity.ReferenceType.API)
                .build();
            Plan plan2 = PlanFixtures.HttpV4.anApiKey()
                .toBuilder()
                .id(PLAN_ID_2)
                .order(2)
                .referenceId(REFERENCE_ID)
                .referenceType(GenericPlanEntity.ReferenceType.API)
                .build();
            planSearchQueryService = mock(PlanSearchQueryService.class);
            when(planSearchQueryService.searchPlans(eq(REFERENCE_ID), eq(REFERENCE_TYPE_API), any(), any(), anyBoolean())).thenReturn(
                List.of(plan1, plan2)
            );
            subscriptionQueryService.initWith(
                List.of(
                    SubscriptionEntity.builder()
                        .id("sub-1")
                        .planId(PLAN_ID_1)
                        .applicationId("app-1")
                        .referenceId(REFERENCE_ID)
                        .referenceType(SubscriptionReferenceType.API)
                        .status(SubscriptionEntity.Status.ACCEPTED)
                        .build()
                )
            );
            getPlansUseCase = new GetPlansUseCase(planSearchQueryService, subscriptionQueryService);

            var input = GetPlansUseCase.Input.forList(REFERENCE_ID, REFERENCE_TYPE_API, "user", true, null, "app-1");
            var output = getPlansUseCase.execute(input);

            assertThat(output.plans()).hasSize(1);
            assertThat(output.plans().get(0).getId()).isEqualTo(PLAN_ID_2);
        }

        @Test
        void should_filter_plans_when_subscribableBy_provided_for_api_product() {
            Plan plan1 = PlanFixtures.aPlanHttpV4()
                .toBuilder()
                .id(PLAN_ID_1)
                .order(1)
                .referenceId(REFERENCE_ID)
                .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                .build();
            Plan plan2 = PlanFixtures.HttpV4.anApiKey()
                .toBuilder()
                .id(PLAN_ID_2)
                .order(2)
                .referenceId(REFERENCE_ID)
                .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                .build();
            planSearchQueryService = mock(PlanSearchQueryService.class);
            when(
                planSearchQueryService.searchPlans(eq(REFERENCE_ID), eq(REFERENCE_TYPE_API_PRODUCT), any(), any(), anyBoolean())
            ).thenReturn(List.of(plan1, plan2));
            subscriptionQueryService.initWith(
                List.of(
                    SubscriptionEntity.builder()
                        .id("sub-1")
                        .planId(PLAN_ID_1)
                        .applicationId("app-1")
                        .referenceId(REFERENCE_ID)
                        .referenceType(SubscriptionReferenceType.API_PRODUCT)
                        .status(SubscriptionEntity.Status.ACCEPTED)
                        .build()
                )
            );
            getPlansUseCase = new GetPlansUseCase(planSearchQueryService, subscriptionQueryService);

            var input = GetPlansUseCase.Input.forList(REFERENCE_ID, REFERENCE_TYPE_API_PRODUCT, "user", true, null, "app-1");
            var output = getPlansUseCase.execute(input);

            assertThat(output.plans()).hasSize(1);
            assertThat(output.plans().get(0).getId()).isEqualTo(PLAN_ID_2);
            assertThat(output.plans().get(0).getReferenceType()).isEqualTo(GenericPlanEntity.ReferenceType.API_PRODUCT);
        }
    }

    @Nested
    class GetSinglePlan {

        @Test
        void should_return_single_plan_when_planId_provided() {
            Plan plan = PlanFixtures.aPlanHttpV4()
                .toBuilder()
                .id(PLAN_ID_1)
                .referenceId(REFERENCE_ID)
                .referenceType(GenericPlanEntity.ReferenceType.API)
                .build();
            PlanCrudServiceInMemory planCrudService = new PlanCrudServiceInMemory();
            planCrudService.initWith(List.of(plan));
            PlanQueryServiceInMemory planQueryService = new PlanQueryServiceInMemory(planCrudService);
            planSearchQueryService = new PlanSearchQueryServiceImpl(planQueryService, planCrudService);
            getPlansUseCase = new GetPlansUseCase(planSearchQueryService, subscriptionQueryService);

            var input = GetPlansUseCase.Input.forSingle(REFERENCE_ID, REFERENCE_TYPE_API, PLAN_ID_1);
            var output = getPlansUseCase.execute(input);

            assertThat(output.plans()).isNull();
            assertThat(output.plan()).isPresent();
            assertThat(output.plan().get().getId()).isEqualTo(PLAN_ID_1);
            assertThat(output.plan().get().getReferenceId()).isEqualTo(REFERENCE_ID);
        }

        @Test
        void should_return_single_plan_for_api_product_reference() {
            Plan plan = PlanFixtures.aPlanHttpV4()
                .toBuilder()
                .id(PLAN_ID_1)
                .referenceId(REFERENCE_ID)
                .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                .build();
            PlanCrudServiceInMemory planCrudService = new PlanCrudServiceInMemory();
            planCrudService.initWith(List.of(plan));
            PlanQueryServiceInMemory planQueryService = new PlanQueryServiceInMemory(planCrudService);
            planSearchQueryService = new PlanSearchQueryServiceImpl(planQueryService, planCrudService);
            getPlansUseCase = new GetPlansUseCase(planSearchQueryService, subscriptionQueryService);

            var input = GetPlansUseCase.Input.forSingle(REFERENCE_ID, REFERENCE_TYPE_API_PRODUCT, PLAN_ID_1);
            var output = getPlansUseCase.execute(input);

            assertThat(output.plan()).isPresent();
            assertThat(output.plan().get().getReferenceId()).isEqualTo(REFERENCE_ID);
        }
    }
}
