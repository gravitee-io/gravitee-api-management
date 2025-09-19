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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import io.gravitee.rest.api.service.v4.PlanSearchService;
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

    private PlanSearchService planSearchService;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private GroupService groupService;

    @Mock
    private ApiSearchService apiSearchService;

    @Mock
    private GenericPlanMapper genericPlanMapper;

    private Plan plan;

    @Mock
    private ObjectMapper objectMapper;

    private Api api;

    @Before
    public void before() throws TechnicalException {
        planSearchService = new PlanSearchServiceImpl(
            planRepository,
            apiRepository,
            groupService,
            apiSearchService,
            objectMapper,
            genericPlanMapper
        );

        api = new Api();
        api.setId(API_ID);
        api.setDefinitionVersion(DefinitionVersion.V4);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        plan = new Plan();
        plan.setId(PLAN_ID);
        plan.setApi(API_ID);
    }

    @Test
    public void shouldFindById() throws TechnicalException {
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));

        PlanEntity planEntity = new PlanEntity();
        when(genericPlanMapper.toGenericPlan(api, plan)).thenReturn(planEntity);

        final GenericPlanEntity resultPlanEntity = planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID);

        assertSame(resultPlanEntity, planEntity);
    }

    @Test
    public void shouldFindByIdIn() throws TechnicalException {
        Plan plan2 = new Plan();
        plan2.setId("plan2");
        plan2.setApi(API_ID);
        Set<String> ids = Set.of(PLAN_ID, "plan2");
        when(planRepository.findByIdIn(argThat(ids::containsAll))).thenReturn(Set.of(plan, plan2));

        apiRepository.findById(plan.getApi());
        PlanEntity planEntity = new PlanEntity();
        planEntity.setId(plan.getId());
        when(genericPlanMapper.toGenericPlan(api, plan)).thenReturn(planEntity);

        PlanEntity planEntity2 = new PlanEntity();
        planEntity2.setId(plan2.getId());
        when(genericPlanMapper.toGenericPlan(api, plan2)).thenReturn(planEntity2);

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
        when(genericPlanMapper.toGenericPlan(api, plan1)).thenReturn(planEntity1);
        when(genericPlanMapper.toGenericPlan(api, plan2)).thenReturn(planEntity2);
        when(planRepository.findByApi(API_ID)).thenReturn(Set.of(plan1, plan2));
        Set<GenericPlanEntity> plans = planSearchService.findByApi(GraviteeContext.getExecutionContext(), API_ID);

        assertNotNull(plans);
        assertEquals(2, plans.size());
    }

    private Plan createPlan(String id) {
        Plan plan = new Plan();
        plan.setId(id);
        plan.setApi(API_ID);
        plan.setType(Plan.PlanType.API);
        plan.setValidation(Plan.PlanValidationType.AUTO);
        return plan;
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindByApiBecauseTechnicalException() throws TechnicalException {
        when(planRepository.findByApi(API_ID)).thenThrow(TechnicalException.class);

        planSearchService.findByApi(GraviteeContext.getExecutionContext(), API_ID);
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
        plan.setApi("api-id");
        when(planRepository.findByIdIn(List.of("plan-id"))).thenReturn(Set.of(plan));
        assertFalse(planSearchService.anyPlanMismatchWithApi(List.of("plan-id"), "api-id"));
    }

    @Test
    public void shouldHaveMismatchPlanForApi() throws TechnicalException {
        Plan plan = new Plan();
        plan.setId("plan-id");
        plan.setApi("api-id");
        Plan plan2 = new Plan();
        plan2.setId("plan2-id");
        plan2.setApi("api2-id");
        when(planRepository.findByIdIn(List.of("plan-id", "plan2-id"))).thenReturn(Set.of(plan, plan2));
        assertTrue(planSearchService.anyPlanMismatchWithApi(List.of("plan-id", "plan2-id"), "api-id"));
    }
}
