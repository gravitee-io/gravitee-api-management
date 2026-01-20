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
        PlanEntity plan1 = fakeV4PlanEntity("plan-1", 3, PlanSecurityType.API_KEY, "{\"nice\": \"config\"}", PlanStatus.PUBLISHED);
        PlanEntity plan2 = fakeV4PlanEntity("plan-2", 2, PlanSecurityType.API_KEY, "{\"nice\": \"config\"}", PlanStatus.STAGING);
        PlanEntity plan3 = fakeV4PlanEntity("plan-3", 1, null, "{\"nice\": \"config\"}", PlanStatus.PUBLISHED);

        var api = new io.gravitee.rest.api.model.v4.api.ApiEntity();
        api.setId(API_ID);
        api.setDefinitionVersion(DefinitionVersion.V4);
        api.setPlans(Set.of(plan1, plan2, plan3));
        when(apiSearchService.findGenericById(eq(GraviteeContext.getExecutionContext()), eq(API_ID), eq(false), eq(false))).thenReturn(api);

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
        var rule = new Rule();
        rule.setMethods(Set.of(HttpMethod.GET));
        rule.setDescription("description of a rule");
        rule.setEnabled(true);
        var rules = List.of(rule);

        var plan1 = fakeV2PlanEntity(
            "plan-1",
            1,
            io.gravitee.rest.api.model.PlanSecurityType.JWT,
            "{\"nice\": \"config\"}",
            io.gravitee.rest.api.model.PlanStatus.DEPRECATED,
            rules
        );
        var plan2 = fakeV2PlanEntity(
            "plan-2",
            2,
            io.gravitee.rest.api.model.PlanSecurityType.JWT,
            "{\"nice\": \"config\"}",
            io.gravitee.rest.api.model.PlanStatus.DEPRECATED,
            null
        );
        var plan3 = fakeV2PlanEntity(
            "plan-3",
            3,
            io.gravitee.rest.api.model.PlanSecurityType.JWT,
            "{\"nice\": \"config\"}",
            io.gravitee.rest.api.model.PlanStatus.STAGING,
            null
        );
        var plan4 = fakeV2PlanEntity(
            "plan-4",
            4,
            io.gravitee.rest.api.model.PlanSecurityType.OAUTH2,
            "{\"nice\": \"config\"}",
            io.gravitee.rest.api.model.PlanStatus.DEPRECATED,
            rules
        );
        var plan5 = fakeV2PlanEntity(
            "plan-5",
            5,
            io.gravitee.rest.api.model.PlanSecurityType.OAUTH2,
            "{\"nice\": \"config\"}",
            io.gravitee.rest.api.model.PlanStatus.STAGING,
            rules
        );

        var api = new io.gravitee.rest.api.model.api.ApiEntity();
        api.setId(API_ID);
        api.setPlans(Set.of(plan1, plan2, plan3, plan4, plan5));
        when(apiSearchService.findGenericById(eq(GraviteeContext.getExecutionContext()), eq(API_ID), eq(false), eq(false))).thenReturn(api);

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
        PlanEntity plan1 = fakeV4PlanEntity("plan-1", 3, PlanSecurityType.API_KEY, "{\"nice\": \"config\"}", PlanStatus.PUBLISHED);
        PlanEntity plan2 = fakeV4PlanEntity("plan-2", 2, PlanSecurityType.API_KEY, "{\"nice\": \"config\"}", PlanStatus.STAGING);
        PlanEntity plan3 = fakeV4PlanEntity("plan-3", 1, null, "{\"nice\": \"config\"}", PlanStatus.PUBLISHED);

        var api = new io.gravitee.rest.api.model.v4.api.ApiEntity();
        api.setId(API_ID);
        api.setDefinitionVersion(DefinitionVersion.V4);
        api.setPlans(Set.of(plan1, plan2, plan3));
        when(apiSearchService.findGenericById(eq(GraviteeContext.getExecutionContext()), eq(API_ID), eq(false), eq(false))).thenReturn(api);

        when(groupService.isUserAuthorizedToAccessApiData(eq(api), any(), eq(USER))).thenReturn(false);

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
}
