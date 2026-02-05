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
package io.gravitee.rest.api.service.v4.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.flow.crud_service.FlowCrudService;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.apiproducts.ApiProductsRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.ApiProduct;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.rest.api.model.v4.plan.*;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.converter.PlanConverter;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import io.gravitee.rest.api.service.v4.FlowService;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import io.gravitee.rest.api.service.v4.mapper.GenericApiMapper;
import io.gravitee.rest.api.service.v4.mapper.GenericPlanMapper;
import io.gravitee.rest.api.service.v4.mapper.PlanMapper;
import java.util.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PlanSearchServiceImpl_SearchTest {

    private static final String API_ID = "my-api";
    private static final String API_PRODUCT_ID = "my-api-product";
    private static final String USER = "my-user";

    private PlanSearchService planSearchService;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private ApiProductsRepository apiProductsRepository;

    @Mock
    private GroupService groupService;

    @Mock
    private ApiSearchService apiSearchService;

    @Mock
    private FlowService flowService;

    @Mock
    private io.gravitee.rest.api.service.configuration.flow.FlowService flowServiceV2;

    @Mock
    private FlowCrudService flowCrudService;

    @Mock
    private GenericApiMapper genericApiMapper;

    @Mock
    private ObjectMapper objectMapper;

    private Api api;

    private GenericPlanMapper genericPlanMapper = null;

    @Before
    public void before() throws TechnicalException {
        GraviteeContext.cleanContext();
        planSearchService = new PlanSearchServiceImpl(
            planRepository,
            apiRepository,
            apiProductsRepository,
            groupService,
            apiSearchService,
            objectMapper,
            genericPlanMapper = new GenericPlanMapper(
                new PlanMapper(),
                flowService,
                new PlanConverter(objectMapper),
                flowServiceV2,
                flowCrudService
            ),
            genericApiMapper
        );
    }

    @Test
    public void should_not_return_anything_if_no_api_id_specified() throws TechnicalException {
        List<GenericPlanEntity> plans = planSearchService.search(
            GraviteeContext.getExecutionContext(),
            PlanQuery.builder().build(),
            USER,
            true,
            true
        );

        assertNotNull(plans);
        assertEquals(0, plans.size());
        verify(apiRepository, never()).findById(anyString());
        verify(planRepository, never()).findByApi(anyString());
    }

    @Test
    public void should_return_list_if_only_api_id_specified_v4() throws TechnicalException {
        api = new Api();
        api.setId(API_ID);
        api.setDefinitionVersion(DefinitionVersion.V4);
        api.setType(ApiType.PROXY);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        var apiEntity = new io.gravitee.rest.api.model.v4.api.ApiEntity();
        apiEntity.setId(API_ID);
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);
        when(
            genericApiMapper.toGenericApi(eq(GraviteeContext.getExecutionContext()), eq(api), eq(null), eq(false), eq(false), eq(false))
        ).thenReturn(apiEntity);

        var plan1 = fakePlanRepository("plan-1", 3, Plan.PlanSecurityType.API_KEY, "{\"nice\": \"config\"}", Plan.Status.PUBLISHED);
        plan1.setReferenceId(API_ID);
        plan1.setReferenceType(Plan.PlanReferenceType.API);
        var plan2 = fakePlanRepository("plan-2", 2, Plan.PlanSecurityType.API_KEY, "{\"nice\": \"config\"}", Plan.Status.STAGING);
        plan2.setReferenceId(API_ID);
        plan2.setReferenceType(Plan.PlanReferenceType.API);
        var plan3 = fakePlanRepository("plan-3", 1, Plan.PlanSecurityType.KEY_LESS, "{\"nice\": \"config\"}", Plan.Status.PUBLISHED);
        plan3.setReferenceId(API_ID);
        plan3.setReferenceType(Plan.PlanReferenceType.API);
        when(planRepository.findByReferenceIdAndReferenceType(API_ID, Plan.PlanReferenceType.API)).thenReturn(Set.of(plan1, plan2, plan3));

        List<GenericPlanEntity> plans = planSearchService.search(
            GraviteeContext.getExecutionContext(),
            PlanQuery.builder()
                .apiId(API_ID)
                .securityType(List.of(PlanSecurityType.API_KEY))
                .referenceId(API_ID)
                .referenceType(GenericPlanEntity.ReferenceType.API)
                .referenceId(API_ID)
                .build(),
            USER,
            true,
            true
        );

        assertNotNull(plans);
        assertEquals(2, plans.size());
    }

    @Test
    public void should_return_list_with_params_specified_v2() throws TechnicalException {
        api = new Api();
        api.setId(API_ID);
        api.setDefinitionVersion(DefinitionVersion.V2);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        var apiEntity = new io.gravitee.rest.api.model.api.ApiEntity();
        apiEntity.setId(API_ID);
        apiEntity.setGraviteeDefinitionVersion(DefinitionVersion.V2.getLabel());
        when(
            genericApiMapper.toGenericApi(eq(GraviteeContext.getExecutionContext()), eq(api), eq(null), eq(false), eq(false), eq(false))
        ).thenReturn(apiEntity);

        var plan1 = fakePlanRepository("plan-1", 1, Plan.PlanSecurityType.JWT, "{\"nice\": \"config\"}", Plan.Status.DEPRECATED);
        plan1.setReferenceId(API_ID);
        plan1.setReferenceType(Plan.PlanReferenceType.API);
        var plan2 = fakePlanRepository("plan-2", 2, Plan.PlanSecurityType.JWT, "{\"nice\": \"config\"}", Plan.Status.DEPRECATED);
        plan2.setReferenceId(API_ID);
        plan2.setReferenceType(Plan.PlanReferenceType.API);
        var plan3 = fakePlanRepository("plan-3", 3, Plan.PlanSecurityType.JWT, "{\"nice\": \"config\"}", Plan.Status.STAGING);
        plan3.setReferenceId(API_ID);
        plan3.setReferenceType(Plan.PlanReferenceType.API);
        var plan4 = fakePlanRepository("plan-4", 4, Plan.PlanSecurityType.OAUTH2, "{\"nice\": \"config\"}", Plan.Status.DEPRECATED);
        plan4.setReferenceId(API_ID);
        plan4.setReferenceType(Plan.PlanReferenceType.API);
        var plan5 = fakePlanRepository("plan-5", 5, Plan.PlanSecurityType.OAUTH2, "{\"nice\": \"config\"}", Plan.Status.STAGING);
        plan5.setReferenceId(API_ID);
        plan5.setReferenceType(Plan.PlanReferenceType.API);
        when(planRepository.findByReferenceIdAndReferenceType(API_ID, Plan.PlanReferenceType.API)).thenReturn(
            Set.of(plan1, plan2, plan3, plan4, plan5)
        );

        List<GenericPlanEntity> plans = planSearchService.search(
            GraviteeContext.getExecutionContext(),
            PlanQuery.builder()
                .apiId(API_ID)
                .referenceId(API_ID)
                .referenceType(GenericPlanEntity.ReferenceType.API)
                .securityType(List.of(PlanSecurityType.JWT))
                .status(List.of(PlanStatus.DEPRECATED))
                .mode(PlanMode.STANDARD)
                .build(),
            USER,
            true,
            true
        );

        assertNotNull(plans);
        assertEquals(2, plans.size());
    }

    @Test
    public void should_return_empty_list_if_not_admin_and_no_access() throws TechnicalException {
        api = new Api();
        api.setId(API_ID);
        api.setDefinitionVersion(DefinitionVersion.V4);
        api.setType(ApiType.PROXY);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        var apiEntity = new io.gravitee.rest.api.model.v4.api.ApiEntity();
        apiEntity.setId(API_ID);
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);
        when(
            genericApiMapper.toGenericApi(eq(GraviteeContext.getExecutionContext()), eq(api), eq(null), eq(false), eq(false), eq(false))
        ).thenReturn(apiEntity);

        var plan1 = fakePlanRepository("plan-1", 3, Plan.PlanSecurityType.API_KEY, "{\"nice\": \"config\"}", Plan.Status.PUBLISHED);
        plan1.setReferenceId(API_ID);
        plan1.setReferenceType(Plan.PlanReferenceType.API);
        var plan2 = fakePlanRepository("plan-2", 2, Plan.PlanSecurityType.API_KEY, "{\"nice\": \"config\"}", Plan.Status.STAGING);
        plan2.setReferenceId(API_ID);
        plan2.setReferenceType(Plan.PlanReferenceType.API);
        var plan3 = fakePlanRepository("plan-3", 1, Plan.PlanSecurityType.KEY_LESS, "{\"nice\": \"config\"}", Plan.Status.PUBLISHED);
        plan3.setReferenceId(API_ID);
        plan3.setReferenceType(Plan.PlanReferenceType.API);
        when(planRepository.findByReferenceIdAndReferenceType(API_ID, Plan.PlanReferenceType.API)).thenReturn(Set.of(plan1, plan2, plan3));

        when(groupService.isUserAuthorizedToAccessApiData(eq(apiEntity), any(), eq(USER))).thenReturn(false);

        List<GenericPlanEntity> plans = planSearchService.search(
            GraviteeContext.getExecutionContext(),
            PlanQuery.builder().apiId(API_ID).referenceId(API_ID).referenceType(GenericPlanEntity.ReferenceType.API).build(),
            USER,
            false,
            true
        );

        assertNotNull(plans);
        assertEquals(0, plans.size());
    }

    private PlanEntity fakeV4PlanEntity(
        String id,
        Integer order,
        PlanSecurityType planSecurityType,
        String securityConfig,
        PlanStatus planStatus
    ) {
        var plan = new PlanEntity();
        plan.setId(id);
        plan.setApiId(API_ID);
        plan.setOrder(order);

        var planSecurity = new PlanSecurity();
        planSecurity.setType(Objects.isNull(planSecurityType) ? null : planSecurityType.getLabel());
        planSecurity.setConfiguration(securityConfig);

        plan.setSecurity(planSecurity);

        plan.setStatus(planStatus);

        return plan;
    }

    private io.gravitee.repository.management.model.Plan fakePlanRepository(
        String id,
        Integer order,
        Plan.PlanSecurityType securityType,
        String securityConfig,
        Plan.Status status
    ) {
        var plan = new io.gravitee.repository.management.model.Plan();
        plan.setId(id);
        plan.setReferenceId(API_ID);
        plan.setType(Plan.PlanType.API);
        plan.setValidation(Plan.PlanValidationType.AUTO);
        plan.setOrder(order);
        plan.setSecurity(securityType);
        plan.setSecurityDefinition(securityConfig);
        plan.setStatus(status);

        return plan;
    }

    @Test
    public void should_return_plans_for_api_product() throws TechnicalException {
        Plan plan1 = createPlan("plan-1");
        Plan plan2 = createPlan("plan-2");
        Plan plan3 = createPlan("plan-3");
        ApiProduct apiProduct = new ApiProduct();
        apiProduct.setId(API_PRODUCT_ID);
        apiProduct.setDescription("description");
        when(apiProductsRepository.findById(API_PRODUCT_ID)).thenReturn(Optional.of(apiProduct));
        when(planRepository.findByReferenceIdAndReferenceType(API_PRODUCT_ID, Plan.PlanReferenceType.API_PRODUCT)).thenReturn(
            Set.of(plan1, plan2, plan3)
        );

        var plan = planSearchService.findByApiProduct(GraviteeContext.getExecutionContext(), API_PRODUCT_ID);
        assertNotNull(plan);
    }

    @Test
    public void should_return_plans_for_api_product_by_id() throws TechnicalException {
        Plan plan1 = createPlan("plan-1");
        ApiProduct apiProduct = new ApiProduct();
        apiProduct.setId(API_PRODUCT_ID);
        apiProduct.setDescription("description");

        when(
            planRepository.findByIdAndReferenceIdAndReferenceType("plan-1", API_PRODUCT_ID, Plan.PlanReferenceType.API_PRODUCT)
        ).thenReturn(Optional.of(plan1));

        var plan = planSearchService.findByPlanIdIdForApiProduct(GraviteeContext.getExecutionContext(), "plan-1", API_PRODUCT_ID);

        assertNotNull(plan);
    }

    @Test
    public void should_return_list_plans_for_api_product() throws TechnicalException {
        ApiProduct apiProduct = new ApiProduct();
        apiProduct.setId(API_PRODUCT_ID);
        apiProduct.setDescription("description");
        when(apiProductsRepository.findById(API_PRODUCT_ID)).thenReturn(Optional.of(apiProduct));
        Plan plan1 = createPlan("plan-1");
        Plan plan2 = createPlan("plan-2");
        Plan plan3 = createPlan("plan-3");
        when(planRepository.findByReferenceIdAndReferenceType(API_PRODUCT_ID, Plan.PlanReferenceType.API_PRODUCT)).thenReturn(
            Set.of(plan1, plan2, plan3)
        );

        var plans = planSearchService.searchForApiProductPlans(
            GraviteeContext.getExecutionContext(),
            PlanQuery.builder().referenceId(API_PRODUCT_ID).referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT).build(),
            USER,
            true
        );
        assertNotNull(plans);
        assertEquals(3, plans.size());
    }

    private Plan createPlan(String id) {
        Plan plan = new Plan();
        plan.setId(id);
        plan.setReferenceId(API_PRODUCT_ID);
        plan.setSecurity(Plan.PlanSecurityType.API_KEY);
        plan.setReferenceType(Plan.PlanReferenceType.API_PRODUCT);
        plan.setValidation(Plan.PlanValidationType.AUTO);
        return plan;
    }
}
