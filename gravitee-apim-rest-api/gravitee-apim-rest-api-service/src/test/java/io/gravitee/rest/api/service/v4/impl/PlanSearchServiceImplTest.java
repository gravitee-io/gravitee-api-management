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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiProductsRepository;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.ApiProduct;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanQuery;
import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import io.gravitee.rest.api.service.v4.mapper.GenericApiMapper;
import io.gravitee.rest.api.service.v4.mapper.GenericPlanMapper;
import java.util.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PlanSearchServiceImplTest {

    private static final String PLAN_ID = "my-plan";
    private static final String API_ID = "my-api";
    private static final String API_PRODUCT_ID = "c45b8e66-4d2a-47ad-9b8e-664d2a97ad88";

    private PlanSearchService planSearchService;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private ApiProductsRepository apiProductRepository;

    @Mock
    private GroupService groupService;

    @Mock
    private ApiSearchService apiSearchService;

    @Mock
    private GenericPlanMapper genericPlanMapper;

    @Mock
    private GenericApiMapper genericApiMapper;

    private Plan plan;

    @Mock
    private ObjectMapper objectMapper;

    private Api api;

    @Before
    public void before() throws TechnicalException {
        planSearchService = new PlanSearchServiceImpl(
            planRepository,
            apiRepository,
            apiProductRepository,
            groupService,
            apiSearchService,
            objectMapper,
            genericPlanMapper,
            genericApiMapper
        );

        api = new Api();
        api.setId(API_ID);
        api.setDefinitionVersion(DefinitionVersion.V4);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        plan = new Plan();
        plan.setId(PLAN_ID);
        plan.setReferenceId(API_ID);
        plan.setReferenceType(Plan.PlanReferenceType.API);
        plan.setReferenceId(API_ID);
    }

    @Test
    public void shouldFindById() throws TechnicalException {
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));

        PlanEntity planEntity = new PlanEntity();
        when(genericPlanMapper.toGenericPlanWithFlow(api, plan)).thenReturn(planEntity);

        final GenericPlanEntity resultPlanEntity = planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID);

        assertSame(resultPlanEntity, planEntity);
    }

    @Test
    public void shouldFindByIdIn() throws TechnicalException {
        Plan plan2 = new Plan();
        plan2.setId("plan2");
        plan2.setReferenceId(API_ID);
        plan2.setReferenceType(Plan.PlanReferenceType.API);
        Set<String> ids = Set.of(PLAN_ID, "plan2");
        when(planRepository.findByIdIn(argThat(ids::containsAll))).thenReturn(Set.of(plan, plan2));

        apiRepository.findById(plan.getReferenceId());
        PlanEntity planEntity = new PlanEntity();
        planEntity.setId(plan.getId());
        when(genericPlanMapper.toGenericPlanWithFlow(api, plan)).thenReturn(planEntity);

        PlanEntity planEntity2 = new PlanEntity();
        planEntity2.setId(plan2.getId());
        when(genericPlanMapper.toGenericPlanWithFlow(api, plan2)).thenReturn(planEntity2);

        final Set<GenericPlanEntity> entities = planSearchService.findByIdIn(GraviteeContext.getExecutionContext(), ids);

        assertEquals(2, entities.size());
        assertTrue(entities.contains(planEntity));
        assertTrue(entities.contains(planEntity2));
    }

    @Test(expected = PlanNotFoundException.class)
    public void shouldNotFindByIdBecauseNotExists() throws TechnicalException {
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.empty());

        planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindByIdBecauseTechnicalException() throws TechnicalException {
        when(planRepository.findById(PLAN_ID)).thenThrow(TechnicalException.class);

        planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID);
    }

    @Test
    public void shouldFindByApi() throws TechnicalException {
        Plan plan1 = createPlan("plan1");
        Plan plan2 = createPlan("plan2");

        PlanEntity planEntity1 = new PlanEntity();
        planEntity1.setId(plan1.getId());
        PlanEntity planEntity2 = new PlanEntity();
        planEntity2.setId(plan2.getId());
        when(genericPlanMapper.toGenericPlansWithFlow(eq(api), eq(Set.of(plan1, plan2)))).thenReturn(Set.of(planEntity1, planEntity2));
        when(planRepository.findByReferenceIdAndReferenceType(API_ID, Plan.PlanReferenceType.API)).thenReturn(Set.of(plan1, plan2));
        Set<GenericPlanEntity> plans = planSearchService.findByApi(GraviteeContext.getExecutionContext(), API_ID, true);

        assertNotNull(plans);
        assertEquals(2, plans.size());
    }

    private Plan createPlan(String id) {
        Plan plan = new Plan();
        plan.setId(id);
        plan.setApi(API_ID);
        plan.setType(Plan.PlanType.API);
        plan.setReferenceType(Plan.PlanReferenceType.API);
        plan.setReferenceId(API_ID);
        plan.setValidation(Plan.PlanValidationType.AUTO);
        return plan;
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindByApiBecauseTechnicalException() throws TechnicalException {
        when(planRepository.findByReferenceIdAndReferenceType(API_ID, Plan.PlanReferenceType.API)).thenThrow(TechnicalException.class);

        planSearchService.findByApi(GraviteeContext.getExecutionContext(), API_ID, true);
    }

    @Test
    public void shouldFindByIdAsMap() throws TechnicalException {
        Plan plan = new Plan();
        plan.setSecurity(Plan.PlanSecurityType.API_KEY);
        plan.setId("my-id");
        plan.setStatus(Plan.Status.STAGING);
        plan.setCrossId("cross-id");
        plan.setOrder(12);
        plan.setName("my-plan-name");
        plan.setDescription("my-plan-description");

        when(objectMapper.convertValue(plan, Map.class)).thenAnswer(i -> new ObjectMapper().convertValue(i.getArgument(0), Map.class));
        when(planRepository.findById("my-id")).thenReturn(Optional.of(plan));

        Map resultMap = planSearchService.findByIdAsMap("my-id");

        assertEquals("my-id", resultMap.get("id"));
        assertEquals("cross-id", resultMap.get("crossId"));
        assertEquals("my-plan-name", resultMap.get("name"));
        assertEquals("my-plan-description", resultMap.get("description"));
        assertEquals("API_KEY", resultMap.get("security"));
    }

    @Test
    public void shouldNotHaveMismatchPlanForApi() throws TechnicalException {
        Plan plan = new Plan();
        plan.setId("plan-id");
        plan.setReferenceId("api-id");
        when(planRepository.findByIdIn(List.of("plan-id"))).thenReturn(Set.of(plan));
        assertFalse(planSearchService.anyPlanMismatchWithApi(List.of("plan-id"), "api-id"));
    }

    @Test
    public void shouldHaveMismatchPlanForApi() throws TechnicalException {
        Plan plan = new Plan();
        plan.setId("plan-id");
        plan.setApi("api-id");
        plan.setReferenceId("api-id");
        plan.setReferenceType(Plan.PlanReferenceType.API);
        Plan plan2 = new Plan();
        plan2.setId("plan2-id");
        plan2.setApi("api2-id");
        plan2.setReferenceId("api2-id");
        plan2.setReferenceType(Plan.PlanReferenceType.API);
        when(planRepository.findByIdIn(List.of("plan-id", "plan2-id"))).thenReturn(Set.of(plan, plan2));
        assertTrue(planSearchService.anyPlanMismatchWithApi(List.of("plan-id", "plan2-id"), "api-id"));
    }

    @Test
    public void shouldFindByPlanIdForApiProduct() throws TechnicalException {
        when(planRepository.findByIdAndReferenceIdAndReferenceType(PLAN_ID, API_PRODUCT_ID, Plan.PlanReferenceType.API_PRODUCT)).thenReturn(
            Optional.of(plan)
        );

        PlanEntity planEntity = new PlanEntity();
        planEntity.setId(PLAN_ID);
        when(genericPlanMapper.toGenericApiProductPlan(plan)).thenReturn(planEntity);

        final GenericPlanEntity result = planSearchService.findByPlanIdIdForApiProduct(
            GraviteeContext.getExecutionContext(),
            PLAN_ID,
            API_PRODUCT_ID
        );

        assertSame(result, planEntity);
    }

    @Test(expected = PlanNotFoundException.class)
    public void shouldNotFindByPlanIdForApiProductBecauseNotExists() throws TechnicalException {
        when(planRepository.findByIdAndReferenceIdAndReferenceType(PLAN_ID, API_PRODUCT_ID, Plan.PlanReferenceType.API_PRODUCT)).thenReturn(
            Optional.empty()
        );
        planSearchService.findByPlanIdIdForApiProduct(GraviteeContext.getExecutionContext(), PLAN_ID, API_PRODUCT_ID);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindByPlanIdForApiProductBecauseTechnicalException() throws TechnicalException {
        when(planRepository.findByIdAndReferenceIdAndReferenceType(PLAN_ID, API_PRODUCT_ID, Plan.PlanReferenceType.API_PRODUCT)).thenThrow(
            TechnicalException.class
        );
        planSearchService.findByPlanIdIdForApiProduct(GraviteeContext.getExecutionContext(), PLAN_ID, API_PRODUCT_ID);
    }

    @Test
    public void shouldFindByApiProduct() throws TechnicalException {
        when(apiProductRepository.findById(API_PRODUCT_ID)).thenReturn(Optional.of(new ApiProduct()));

        Plan plan1 = createPlan("plan1");
        Plan plan2 = createPlan("plan2");
        when(planRepository.findByReferenceIdAndReferenceType(API_PRODUCT_ID, Plan.PlanReferenceType.API_PRODUCT)).thenReturn(
            Set.of(plan1, plan2)
        );

        PlanEntity planEntity1 = new PlanEntity();
        planEntity1.setId(plan1.getId());
        PlanEntity planEntity2 = new PlanEntity();
        planEntity2.setId(plan2.getId());
        when(genericPlanMapper.toGenericApiProductPlan(plan1)).thenReturn(planEntity1);
        when(genericPlanMapper.toGenericApiProductPlan(plan2)).thenReturn(planEntity2);

        Set<GenericPlanEntity> plans = planSearchService.findByApiProduct(GraviteeContext.getExecutionContext(), API_PRODUCT_ID);

        assertNotNull(plans);
        assertEquals(2, plans.size());
        assertTrue(plans.contains(planEntity1));
        assertTrue(plans.contains(planEntity2));
    }

    @Test
    public void shouldReturnEmptySetIfApiProductNotFound() throws TechnicalException {
        when(apiProductRepository.findById(API_PRODUCT_ID)).thenReturn(Optional.empty());

        Set<GenericPlanEntity> plans = planSearchService.findByApiProduct(GraviteeContext.getExecutionContext(), API_PRODUCT_ID);

        assertNotNull(plans);
        assertTrue(plans.isEmpty());
        verify(planRepository, never()).findByReferenceIdAndReferenceType(anyString(), any());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindByApiProductBecauseTechnicalException() throws TechnicalException {
        when(apiProductRepository.findById(API_PRODUCT_ID)).thenReturn(Optional.of(new ApiProduct()));
        when(planRepository.findByReferenceIdAndReferenceType(API_PRODUCT_ID, Plan.PlanReferenceType.API_PRODUCT)).thenThrow(
            TechnicalException.class
        );

        planSearchService.findByApiProduct(GraviteeContext.getExecutionContext(), API_PRODUCT_ID);
    }

    @Test
    public void searchForApiProductPlans_should_filter_by_security_status_mode_and_name() throws TechnicalException {
        when(apiProductRepository.findById(API_PRODUCT_ID)).thenReturn(Optional.of(new ApiProduct()));

        Plan plan1 = createPlan("plan1");
        Plan plan2 = createPlan("plan2");
        Plan plan3 = createPlan("plan3");
        when(planRepository.findByReferenceIdAndReferenceType(API_PRODUCT_ID, Plan.PlanReferenceType.API_PRODUCT)).thenReturn(
            Set.of(plan1, plan2, plan3)
        );

        var p1 = PlanEntity.builder()
            .id("plan1")
            .name("Gold")
            .mode(io.gravitee.definition.model.v4.plan.PlanMode.STANDARD)
            .status(io.gravitee.definition.model.v4.plan.PlanStatus.PUBLISHED)
            .security(io.gravitee.definition.model.v4.plan.PlanSecurity.builder().type("api-key").configuration("{}").build())
            .build();
        var p2 = PlanEntity.builder()
            .id("plan2")
            .name("Silver")
            .mode(io.gravitee.definition.model.v4.plan.PlanMode.STANDARD)
            .status(io.gravitee.definition.model.v4.plan.PlanStatus.STAGING)
            .security(io.gravitee.definition.model.v4.plan.PlanSecurity.builder().type("key-less").configuration("{}").build())
            .build();
        var p3 = PlanEntity.builder()
            .id("plan3")
            .name("Gold")
            .mode(io.gravitee.definition.model.v4.plan.PlanMode.STANDARD)
            .status(io.gravitee.definition.model.v4.plan.PlanStatus.PUBLISHED)
            .security(null)
            .build();

        when(genericPlanMapper.toGenericApiProductPlan(plan1)).thenReturn(p1);
        when(genericPlanMapper.toGenericApiProductPlan(plan2)).thenReturn(p2);
        when(genericPlanMapper.toGenericApiProductPlan(plan3)).thenReturn(p3);

        List<GenericPlanEntity> plans = planSearchService.searchForApiProductPlans(
            GraviteeContext.getExecutionContext(),
            PlanQuery.builder()
                .referenceId(API_PRODUCT_ID)
                .name("Gold")
                .mode(io.gravitee.definition.model.v4.plan.PlanMode.STANDARD)
                .securityType(List.of(PlanSecurityType.API_KEY))
                .status(List.of(io.gravitee.definition.model.v4.plan.PlanStatus.PUBLISHED))
                .build(),
            "user",
            true
        );

        assertNotNull(plans);
        assertEquals(1, plans.size());
        assertSame(p1, plans.get(0));
    }
}
