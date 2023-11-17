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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.exception.NotFoundDomainException;
import io.gravitee.apim.core.plan.query_service.PlanQueryService;
import io.gravitee.apim.infra.query_service.plan.PlanQueryServiceImpl;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Plan;
import java.util.Optional;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PlanQueryServiceImplTest {

    PlanRepository planRepository;
    PlanQueryService service;

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

            var res = service.findAllByApiIdAndGeneralConditionsAndIsActive(
                API_ID,
                io.gravitee.apim.core.api.model.Api.DefinitionVersion.V4,
                PAGE_ID
            );
            assertThat(res).hasSize(1);
            assertThat(res.get(0).getId()).isEqualTo("published-id");
            assertThat(res.get(0).getPlanSecurity()).isEqualTo(PlanSecurity.builder().type("api-key").build());
            assertThat(res.get(0).getApiId()).isEqualTo(API_ID);
        }

        @Test
        @SneakyThrows
        void should_return_empty_list_if_no_results() {
            when(planRepository.findByApi(eq(API_ID))).thenReturn(Set.of());

            var res = service.findAllByApiIdAndGeneralConditionsAndIsActive(
                API_ID,
                io.gravitee.apim.core.api.model.Api.DefinitionVersion.V4,
                PAGE_ID
            );
            assertThat(res).hasSize(0);
        }
    }
}
