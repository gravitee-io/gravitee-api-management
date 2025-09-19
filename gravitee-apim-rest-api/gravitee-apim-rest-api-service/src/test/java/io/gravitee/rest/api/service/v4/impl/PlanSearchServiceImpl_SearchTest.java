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
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Rule;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.model.v4.plan.*;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import io.gravitee.rest.api.service.v4.mapper.GenericPlanMapper;
import java.util.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PlanSearchServiceImpl_SearchTest {

    private static final String PLAN_ID = "my-plan";
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
    private GenericPlanMapper genericPlanMapper;

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
            genericPlanMapper
        );

        api = new Api();
        api.setId(API_ID);
        api.setDefinitionVersion(DefinitionVersion.V4);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
    }

    @Test
    public void should_not_return_anything_if_no_api_id_specified() throws TechnicalException {
        List<GenericPlanEntity> plans = planSearchService.search(
            GraviteeContext.getExecutionContext(),
            PlanQuery.builder().build(),
            USER,
            true
        );

        assertNotNull(plans);
        assertEquals(0, plans.size());
        verify(apiRepository, never()).findById(anyString());
        verify(planRepository, never()).findByApi(anyString());
    }

    @Test
    public void should_return_list_if_only_api_id_specified_v4() throws TechnicalException {
        Plan plan1 = createPlan("plan-1");
        Plan plan2 = createPlan("plan-2");
        Plan plan3 = createPlan("plan-3");
        when(planRepository.findByApi(API_ID)).thenReturn(Set.of(plan1, plan2, plan3));

        when(genericPlanMapper.toGenericPlan(api, plan1)).thenReturn(
            fakeV4PlanEntity("plan-1", 3, PlanSecurityType.API_KEY, "{\"nice\": \"config\"}", PlanStatus.PUBLISHED)
        );
        when(genericPlanMapper.toGenericPlan(api, plan2)).thenReturn(
            fakeV4PlanEntity("plan-2", 2, PlanSecurityType.API_KEY, "{\"nice\": \"config\"}", PlanStatus.STAGING)
        );
        when(genericPlanMapper.toGenericPlan(api, plan3)).thenReturn(
            fakeV4PlanEntity("plan-3", 1, null, "{\"nice\": \"config\"}", PlanStatus.PUBLISHED)
        );

        List<GenericPlanEntity> plans = planSearchService.search(
            GraviteeContext.getExecutionContext(),
            PlanQuery.builder().apiId(API_ID).securityType(List.of(PlanSecurityType.API_KEY)).build(),
            USER,
            true
        );

        assertNotNull(plans);
        assertEquals(2, plans.size());
    }

    @Test
    public void should_return_list_with_params_specified_v2() throws TechnicalException {
        Plan plan1 = createPlan("plan-1");
        Plan plan2 = createPlan("plan-2");
        Plan plan3 = createPlan("plan-3");
        Plan plan4 = createPlan("plan-4");
        Plan plan5 = createPlan("plan-5");
        when(planRepository.findByApi(API_ID)).thenReturn(Set.of(plan1, plan2, plan3, plan4, plan5));

        var rule = new Rule();
        rule.setMethods(Set.of(HttpMethod.GET));
        rule.setDescription("description of a rule");
        rule.setEnabled(true);
        var rules = List.of(rule);

        when(genericPlanMapper.toGenericPlan(api, plan1)).thenReturn(
            fakeV2PlanEntity(
                "plan-1",
                1,
                io.gravitee.rest.api.model.PlanSecurityType.JWT,
                "{\"nice\": \"config\"}",
                io.gravitee.rest.api.model.PlanStatus.DEPRECATED,
                rules
            )
        );
        when(genericPlanMapper.toGenericPlan(api, plan2)).thenReturn(
            fakeV2PlanEntity(
                "plan-2",
                2,
                io.gravitee.rest.api.model.PlanSecurityType.JWT,
                "{\"nice\": \"config\"}",
                io.gravitee.rest.api.model.PlanStatus.DEPRECATED,
                null
            )
        );
        when(genericPlanMapper.toGenericPlan(api, plan3)).thenReturn(
            fakeV2PlanEntity(
                "plan-3",
                3,
                io.gravitee.rest.api.model.PlanSecurityType.JWT,
                "{\"nice\": \"config\"}",
                io.gravitee.rest.api.model.PlanStatus.STAGING,
                null
            )
        );
        when(genericPlanMapper.toGenericPlan(api, plan4)).thenReturn(
            fakeV2PlanEntity(
                "plan-4",
                4,
                io.gravitee.rest.api.model.PlanSecurityType.OAUTH2,
                "{\"nice\": \"config\"}",
                io.gravitee.rest.api.model.PlanStatus.DEPRECATED,
                rules
            )
        );
        when(genericPlanMapper.toGenericPlan(api, plan5)).thenReturn(
            fakeV2PlanEntity(
                "plan-5",
                5,
                io.gravitee.rest.api.model.PlanSecurityType.OAUTH2,
                "{\"nice\": \"config\"}",
                io.gravitee.rest.api.model.PlanStatus.STAGING,
                rules
            )
        );

        GenericApiEntity api = new io.gravitee.rest.api.model.api.ApiEntity();
        api.setId(API_ID);
        when(apiSearchService.findGenericById(eq(GraviteeContext.getExecutionContext()), eq(API_ID))).thenReturn(api);

        List<GenericPlanEntity> plans = planSearchService.search(
            GraviteeContext.getExecutionContext(),
            PlanQuery.builder()
                .apiId(API_ID)
                .securityType(List.of(PlanSecurityType.JWT))
                .status(List.of(PlanStatus.DEPRECATED))
                .mode(PlanMode.STANDARD)
                .build(),
            USER,
            true
        );

        assertNotNull(plans);
        assertEquals(2, plans.size());
    }

    @Test
    public void should_return_empty_list_if_not_admin_and_no_access() throws TechnicalException {
        Plan plan1 = createPlan("plan-1");
        Plan plan2 = createPlan("plan-2");
        Plan plan3 = createPlan("plan-3");
        when(planRepository.findByApi(API_ID)).thenReturn(Set.of(plan1, plan2, plan3));

        when(genericPlanMapper.toGenericPlan(api, plan1)).thenReturn(
            fakeV4PlanEntity("plan-1", 3, PlanSecurityType.API_KEY, "{\"nice\": \"config\"}", PlanStatus.PUBLISHED)
        );
        when(genericPlanMapper.toGenericPlan(api, plan2)).thenReturn(
            fakeV4PlanEntity("plan-2", 2, PlanSecurityType.API_KEY, "{\"nice\": \"config\"}", PlanStatus.STAGING)
        );
        when(genericPlanMapper.toGenericPlan(api, plan3)).thenReturn(
            fakeV4PlanEntity("plan-3", 1, null, "{\"nice\": \"config\"}", PlanStatus.PUBLISHED)
        );

        GenericApiEntity api = new io.gravitee.rest.api.model.api.ApiEntity();
        api.setId(API_ID);
        when(apiSearchService.findGenericById(eq(GraviteeContext.getExecutionContext()), eq(API_ID))).thenReturn(api);

        when(groupService.isUserAuthorizedToAccessApiData(eq(api), any(), eq(USER))).thenReturn(false);

        List<GenericPlanEntity> plans = planSearchService.search(
            GraviteeContext.getExecutionContext(),
            PlanQuery.builder().apiId(API_ID).build(),
            USER,
            false
        );

        assertNotNull(plans);
        assertEquals(0, plans.size());
    }

    private Plan createPlan(String id) {
        Plan plan = new Plan();
        plan.setId(id);
        plan.setApi(API_ID);
        plan.setType(Plan.PlanType.API);
        plan.setValidation(Plan.PlanValidationType.AUTO);
        return plan;
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
}
