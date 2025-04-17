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
package io.gravitee.apim.infra.query_service.page;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.infra.query_service.plan.PlanQueryServiceImpl;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Plan;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PlanQueryServiceImplTest {

    PlanRepository planRepository;
    PlanQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        planRepository = mock(PlanRepository.class);
        service = new PlanQueryServiceImpl(planRepository);
    }

    @Nested
    class FindAllByGeneralConditionsAndIsActive {

        String API_ID = "api-id";
        String PAGE_ID = "page-id";

        @Test
        @SneakyThrows
        void search_should_return_matching_pages() {
            Plan plan_published = Plan
                .builder()
                .id("published-id")
                .api(API_ID)
                .generalConditions(PAGE_ID)
                .status(Plan.Status.PUBLISHED)
                .security(Plan.PlanSecurityType.API_KEY)
                .build();
            Plan plan_closed = Plan.builder().id("closed-id").api(API_ID).generalConditions(PAGE_ID).status(Plan.Status.CLOSED).build();
            Plan plan_staging = Plan.builder().id("staging-id").api(API_ID).generalConditions(PAGE_ID).status(Plan.Status.STAGING).build();
            Plan plan_different_page = Plan
                .builder()
                .id("different-page-id")
                .api(API_ID)
                .generalConditions("another-page")
                .status(Plan.Status.PUBLISHED)
                .build();

            when(planRepository.findByApi(eq(API_ID))).thenReturn(Set.of(plan_published, plan_closed, plan_staging, plan_different_page));

            var res = service.findAllByApiIdAndGeneralConditionsAndIsActive(API_ID, DefinitionVersion.V4, PAGE_ID);
            assertThat(res).hasSize(1);
            assertThat(res.getFirst().getId()).isEqualTo("published-id");
            assertThat(res.getFirst().getPlanSecurity()).isEqualTo(PlanSecurity.builder().type("API_KEY").build());
            assertThat(res.getFirst().getApiId()).isEqualTo(API_ID);
        }

        @Test
        @SneakyThrows
        void should_return_empty_list_if_no_results() {
            when(planRepository.findByApi(eq(API_ID))).thenReturn(Set.of());

            var res = service.findAllByApiIdAndGeneralConditionsAndIsActive(API_ID, DefinitionVersion.V4, PAGE_ID);
            assertThat(res).isEmpty();
        }
    }

    @Nested
    class FindAllByApiId {

        String API_ID = "api-id";

        @Test
        @SneakyThrows
        void should_return_all_plans_of_an_api() {
            Plan plan1 = Plan
                .builder()
                .id("plan1")
                .api(API_ID)
                .status(Plan.Status.PUBLISHED)
                .security(Plan.PlanSecurityType.API_KEY)
                .build();
            Plan plan2 = Plan.builder().id("plan2").api(API_ID).security(Plan.PlanSecurityType.API_KEY).status(Plan.Status.CLOSED).build();
            Plan plan3 = Plan.builder().id("plan3").api(API_ID).security(Plan.PlanSecurityType.API_KEY).status(Plan.Status.STAGING).build();

            when(planRepository.findByApi(eq(API_ID))).thenReturn(Set.of(plan1, plan2, plan3));

            var res = service.findAllByApiId(API_ID);
            assertThat(res).hasSize(3).extracting(io.gravitee.apim.core.plan.model.Plan::getId).containsOnly("plan1", "plan2", "plan3");
        }

        @Test
        @SneakyThrows
        void should_return_empty_list_if_no_results() {
            when(planRepository.findByApi(eq(API_ID))).thenReturn(Set.of());

            var res = service.findAllByApiId(API_ID);
            assertThat(res).isEmpty();
        }
    }
}
