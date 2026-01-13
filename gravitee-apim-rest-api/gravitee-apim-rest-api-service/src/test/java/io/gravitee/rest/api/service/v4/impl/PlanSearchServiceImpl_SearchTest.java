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
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.flow.crud_service.FlowCrudService;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Rule;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Api;
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
    private static final String USER = "my-user";

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

    @Before
    public void before() throws TechnicalException {
        GraviteeContext.cleanContext();
        planSearchService = new PlanSearchServiceImpl(
            planRepository,
            apiRepository,
            groupService,
            apiSearchService,
            objectMapper,
            new GenericPlanMapper(new PlanMapper(), flowService, new PlanConverter(objectMapper), flowServiceV2, flowCrudService),
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
        var plan2 = fakePlanRepository("plan-2", 2, Plan.PlanSecurityType.API_KEY, "{\"nice\": \"config\"}", Plan.Status.STAGING);
        var plan3 = fakePlanRepository("plan-3", 1, Plan.PlanSecurityType.KEY_LESS, "{\"nice\": \"config\"}", Plan.Status.PUBLISHED);
        when(planRepository.findByApi(API_ID)).thenReturn(Set.of(plan1, plan2, plan3));

        List<GenericPlanEntity> plans = planSearchService.search(
            GraviteeContext.getExecutionContext(),
            PlanQuery.builder().apiId(API_ID).securityType(List.of(PlanSecurityType.API_KEY)).build(),
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
        var plan2 = fakePlanRepository("plan-2", 2, Plan.PlanSecurityType.JWT, "{\"nice\": \"config\"}", Plan.Status.DEPRECATED);
        var plan3 = fakePlanRepository("plan-3", 3, Plan.PlanSecurityType.JWT, "{\"nice\": \"config\"}", Plan.Status.STAGING);
        var plan4 = fakePlanRepository("plan-4", 4, Plan.PlanSecurityType.OAUTH2, "{\"nice\": \"config\"}", Plan.Status.DEPRECATED);
        var plan5 = fakePlanRepository("plan-5", 5, Plan.PlanSecurityType.OAUTH2, "{\"nice\": \"config\"}", Plan.Status.STAGING);
        when(planRepository.findByApi(API_ID)).thenReturn(Set.of(plan1, plan2, plan3, plan4, plan5));

        List<GenericPlanEntity> plans = planSearchService.search(
            GraviteeContext.getExecutionContext(),
            PlanQuery.builder()
                .apiId(API_ID)
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
        var plan2 = fakePlanRepository("plan-2", 2, Plan.PlanSecurityType.API_KEY, "{\"nice\": \"config\"}", Plan.Status.STAGING);
        var plan3 = fakePlanRepository("plan-3", 1, Plan.PlanSecurityType.KEY_LESS, "{\"nice\": \"config\"}", Plan.Status.PUBLISHED);
        when(planRepository.findByApi(API_ID)).thenReturn(Set.of(plan1, plan2, plan3));

        when(groupService.isUserAuthorizedToAccessApiData(eq(apiEntity), any(), eq(USER))).thenReturn(false);

        List<GenericPlanEntity> plans = planSearchService.search(
            GraviteeContext.getExecutionContext(),
            PlanQuery.builder().apiId(API_ID).build(),
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

    private io.gravitee.rest.api.model.PlanEntity fakeV2PlanEntity(
        String id,
        Integer order,
        io.gravitee.rest.api.model.PlanSecurityType planSecurityType,
        String securityConfig,
        io.gravitee.rest.api.model.PlanStatus planStatus,
        List<Rule> rules
    ) {
        var plan = new io.gravitee.rest.api.model.PlanEntity();
        plan.setId(id);
        plan.setOrder(order);
        plan.setApi(API_ID);

        plan.setSecurity(planSecurityType);
        plan.setSecurityDefinition(securityConfig);

        plan.setStatus(planStatus);

        if (Objects.nonNull(rules)) {
            var paths = new HashMap<String, List<Rule>>();
            paths.put("path", rules);
            plan.setPaths(paths);
        }

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
        plan.setApi(API_ID);
        plan.setType(Plan.PlanType.API);
        plan.setValidation(Plan.PlanValidationType.AUTO);
        plan.setOrder(order);
        plan.setSecurity(securityType);
        plan.setSecurityDefinition(securityConfig);
        plan.setStatus(status);

        return plan;
    }
}
