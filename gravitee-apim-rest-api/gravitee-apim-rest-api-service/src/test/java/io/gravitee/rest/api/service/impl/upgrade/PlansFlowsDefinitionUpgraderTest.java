/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.impl.upgrade;

import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.rest.api.service.configuration.flow.FlowService;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class PlansFlowsDefinitionUpgraderTest {

    private static final String API_ID = "API_ID";

    @InjectMocks
    @Spy
    private PlansFlowsDefinitionUpgrader upgrader = new PlansFlowsDefinitionUpgrader();

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private FlowService flowService;

    @Before
    public void setup() {
        ReflectionTestUtils.setField(upgrader, "objectMapper", new ObjectMapper());
    }

    @Test
    public void processOneShotUpgrade_should_convert_flows_of_every_v2_apis() throws Exception {
        doNothing().when(upgrader).migrateApiFlows(any(), any());

        Api api1 = buildApi("api1", "1.0.0");
        Api api2 = buildApi("api2", "2.0.0");
        Api api3 = buildApi("api3", "1.0.0");
        Api api4 = buildApi("api4", "2.0.0");
        when(apiRepository.findAll()).thenReturn(Set.of(api1, api2, api3, api4));

        upgrader.processOneShotUpgrade();

        verify(upgrader, times(1)).migrateApiFlows(eq("api2"), argThat(api -> api.getId().equals("api2")));
        verify(upgrader, times(1)).migrateApiFlows(eq("api4"), argThat(api -> api.getId().equals("api4")));
        verify(upgrader, times(1)).processOneShotUpgrade();
        verifyNoMoreInteractions(upgrader);
    }

    @Test
    public void migrateApiFlows_should_convert_flows_for_each_plan_in_definition_having_flows_and_which_exists_in_api() throws Exception {
        // API has 3 plans in database : plan1, plan2, plan4 and plan5
        Plan plan1 = buildPlan("plan1");
        Plan plan2 = buildPlan("plan2");
        Plan plan4 = buildPlan("plan4");
        Plan plan5 = buildPlan("plan5");
        when(planRepository.findByApi(API_ID)).thenReturn(Set.of(plan1, plan2, plan4, plan5));

        // API has 3 plans in definition : plan1, plan3, plan4 and plan5
        io.gravitee.definition.model.Api apiDefinition = new io.gravitee.definition.model.Api();
        apiDefinition.setId(API_ID);
        io.gravitee.definition.model.Plan definitionPlan1 = buildDefinitionPlan("plan1", List.of(buildFlow("flow1"), buildFlow("flow2")));
        io.gravitee.definition.model.Plan definitionPlan3 = buildDefinitionPlan("plan3", List.of(buildFlow("flow3")));
        io.gravitee.definition.model.Plan definitionPlan4 = buildDefinitionPlan("plan4", List.of());
        io.gravitee.definition.model.Plan definitionPlan5 = buildDefinitionPlan("plan5", List.of(buildFlow("flow4")));
        apiDefinition.setPlans(List.of(definitionPlan1, definitionPlan3, definitionPlan4, definitionPlan5));

        // API has 2 api flows in definition : flow5 and flow6
        List<Flow> apiFlows = List.of(buildFlow("flow5"), buildFlow("flow6"));
        apiDefinition.setFlows(apiFlows);

        upgrader.migrateApiFlows(API_ID, apiDefinition);

        // should save plan flows of plan1 and plan5 in database
        // not plan2 cause it is not in definition
        // not plan3 cause it is not in database
        // not plan4 cause it has no flows
        verify(flowService, times(1)).save(FlowReferenceType.PLAN, plan1.getId(), definitionPlan1.getFlows());
        verify(flowService, times(1)).save(FlowReferenceType.PLAN, plan5.getId(), definitionPlan5.getFlows());

        // should save api flows in database
        verify(flowService, times(1)).save(FlowReferenceType.API, API_ID, apiFlows);

        verifyNoMoreInteractions(flowService);
    }

    private Api buildApi(String id, String definitionVersion) {
        Api api = new Api();
        api.setId(id);
        api.setDefinition("{\"id\": \"" + id + "\", \"gravitee\": \"" + definitionVersion + "\"}");
        return api;
    }

    private Plan buildPlan(String id) {
        Plan plan = new Plan();
        plan.setId(id);
        return plan;
    }

    private io.gravitee.definition.model.Plan buildDefinitionPlan(String id, List<Flow> flows) {
        io.gravitee.definition.model.Plan plan = new io.gravitee.definition.model.Plan();
        plan.setId(id);
        plan.setFlows(flows);
        return plan;
    }

    private Flow buildFlow(String name) {
        Flow flow = new Flow();
        flow.setName(name);
        return flow;
    }
}
