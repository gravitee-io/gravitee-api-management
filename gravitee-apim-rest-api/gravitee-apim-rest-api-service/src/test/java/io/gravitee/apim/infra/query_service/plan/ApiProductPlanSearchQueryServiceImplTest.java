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
package io.gravitee.apim.infra.query_service.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.apiproducts.ApiProductsRepository;
import io.gravitee.repository.management.model.ApiProduct;
import io.gravitee.rest.api.model.v4.plan.PlanQuery;
import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiProductPlanSearchQueryServiceImplTest {

    private static final String API_PRODUCT_ID = "c45b8e66-4d2a-47ad-9b8e-664d2a97ad88";

    @Mock
    ApiProductsRepository apiProductsRepository;

    @Mock
    PlanRepository planRepository;

    ApiProductPlanSearchQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ApiProductPlanSearchQueryServiceImpl(apiProductsRepository, planRepository);
    }

    @Test
    void findByPlanIdIdForApiProduct_should_map_repository_plan() throws Exception {
        var repositoryPlan = io.gravitee.repository.management.model.Plan.builder()
            .id("plan-1")
            .name("Gold")
            .definitionVersion(DefinitionVersion.V4)
            .apiType(ApiType.PROXY)
            .mode(io.gravitee.repository.management.model.Plan.PlanMode.STANDARD)
            .status(io.gravitee.repository.management.model.Plan.Status.PUBLISHED)
            .security(io.gravitee.repository.management.model.Plan.PlanSecurityType.API_KEY)
            .securityDefinition("{\"x\":1}")
            .referenceId(API_PRODUCT_ID)
            .referenceType(io.gravitee.repository.management.model.Plan.PlanReferenceType.API_PRODUCT)
            .build();

        when(
            planRepository.findByIdAndReferenceIdAndReferenceType(
                eq("plan-1"),
                eq(API_PRODUCT_ID),
                eq(io.gravitee.repository.management.model.Plan.PlanReferenceType.API_PRODUCT)
            )
        ).thenReturn(Optional.of(repositoryPlan));

        Plan result = service.findByPlanIdIdForApiProduct("plan-1", API_PRODUCT_ID);

        assertThat(result.getId()).isEqualTo("plan-1");
        assertThat(result.getName()).isEqualTo("Gold");
    }

    @Test
    void findByPlanIdIdForApiProduct_should_throw_when_missing() throws Exception {
        when(
            planRepository.findByIdAndReferenceIdAndReferenceType(
                eq("plan-404"),
                eq(API_PRODUCT_ID),
                eq(io.gravitee.repository.management.model.Plan.PlanReferenceType.API_PRODUCT)
            )
        ).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByPlanIdIdForApiProduct("plan-404", API_PRODUCT_ID)).isInstanceOf(PlanNotFoundException.class);
    }

    @Test
    void searchForApiProductPlans_should_return_empty_when_api_product_does_not_exist() throws Exception {
        when(apiProductsRepository.findById(eq(API_PRODUCT_ID))).thenReturn(Optional.empty());

        var query = PlanQuery.builder().referenceId(API_PRODUCT_ID).build();
        var result = service.searchForApiProductPlans(API_PRODUCT_ID, query, "user", true);

        assertThat(result).isEmpty();
        verify(apiProductsRepository).findById(eq(API_PRODUCT_ID));
        verifyNoInteractions(planRepository);
    }

    @Test
    void searchForApiProductPlans_should_filter_by_name_security_status_and_mode() throws Exception {
        when(apiProductsRepository.findById(eq(API_PRODUCT_ID))).thenReturn(Optional.of(ApiProduct.builder().id(API_PRODUCT_ID).build()));

        // matches all filters
        var plan1 = io.gravitee.repository.management.model.Plan.builder()
            .id("p1")
            .name("Gold")
            .definitionVersion(DefinitionVersion.V4)
            .apiType(ApiType.PROXY)
            .mode(io.gravitee.repository.management.model.Plan.PlanMode.STANDARD)
            .status(io.gravitee.repository.management.model.Plan.Status.PUBLISHED)
            .security(io.gravitee.repository.management.model.Plan.PlanSecurityType.API_KEY)
            .securityDefinition("{\"x\":1}")
            .referenceId(API_PRODUCT_ID)
            .referenceType(io.gravitee.repository.management.model.Plan.PlanReferenceType.API_PRODUCT)
            .build();

        // filtered by name
        var plan2 = io.gravitee.repository.management.model.Plan.builder()
            .id("p2")
            .name("Silver")
            .definitionVersion(DefinitionVersion.V4)
            .apiType(ApiType.PROXY)
            .mode(io.gravitee.repository.management.model.Plan.PlanMode.STANDARD)
            .status(io.gravitee.repository.management.model.Plan.Status.PUBLISHED)
            .security(io.gravitee.repository.management.model.Plan.PlanSecurityType.API_KEY)
            .securityDefinition("{\"x\":1}")
            .referenceId(API_PRODUCT_ID)
            .referenceType(io.gravitee.repository.management.model.Plan.PlanReferenceType.API_PRODUCT)
            .build();

        // mode PUSH => PlanSecurity becomes null via adapter => filtered out by securityType check
        var plan3 = io.gravitee.repository.management.model.Plan.builder()
            .id("p3")
            .name("Gold")
            .definitionVersion(DefinitionVersion.V4)
            .apiType(ApiType.PROXY)
            .mode(io.gravitee.repository.management.model.Plan.PlanMode.PUSH)
            .status(io.gravitee.repository.management.model.Plan.Status.PUBLISHED)
            .security(io.gravitee.repository.management.model.Plan.PlanSecurityType.API_KEY)
            .securityDefinition("{\"x\":1}")
            .referenceId(API_PRODUCT_ID)
            .referenceType(io.gravitee.repository.management.model.Plan.PlanReferenceType.API_PRODUCT)
            .build();

        when(
            planRepository.findByReferenceIdAndReferenceType(
                eq(API_PRODUCT_ID),
                eq(io.gravitee.repository.management.model.Plan.PlanReferenceType.API_PRODUCT)
            )
        ).thenReturn(Set.of(plan1, plan2, plan3));

        var query = PlanQuery.builder()
            .referenceId(API_PRODUCT_ID)
            .name("Gold")
            .securityType(List.of(PlanSecurityType.API_KEY))
            .status(List.of(PlanStatus.PUBLISHED))
            .mode(PlanMode.STANDARD)
            .build();

        var result = service.searchForApiProductPlans(API_PRODUCT_ID, query, "user", true);

        assertThat(result).extracting(Plan::getId).containsExactly("p1");
    }

    @Test
    void searchForApiProductPlans_should_wrap_repository_exception() throws Exception {
        when(apiProductsRepository.findById(eq(API_PRODUCT_ID))).thenThrow(new TechnicalException("boom"));

        var query = PlanQuery.builder().referenceId(API_PRODUCT_ID).build();

        assertThatThrownBy(() -> service.searchForApiProductPlans(API_PRODUCT_ID, query, "user", true)).isInstanceOf(
            TechnicalManagementException.class
        );
    }
}
