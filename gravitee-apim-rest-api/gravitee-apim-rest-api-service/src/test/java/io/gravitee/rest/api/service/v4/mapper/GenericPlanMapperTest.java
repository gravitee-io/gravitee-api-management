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
package io.gravitee.rest.api.service.v4.mapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.flow.crud_service.FlowCrudService;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.nativeapi.NativeFlow;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.rest.api.service.converter.PlanConverter;
import io.gravitee.rest.api.service.v4.FlowService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class GenericPlanMapperTest {

    @Mock
    private PlanMapper planMapper;

    @Mock
    private FlowService flowServiceV4;

    @Mock
    private PlanConverter planConverter;

    @Mock
    private io.gravitee.rest.api.service.configuration.flow.FlowService flowService;

    @Mock
    private FlowCrudService flowCrudService;

    private GenericPlanMapper genericPlanMapper;

    @Before
    public void before() {
        genericPlanMapper = new GenericPlanMapper(planMapper, flowServiceV4, planConverter, flowService, flowCrudService);
    }

    @Test
    public void shouldCallV4PlanMapperWhenDefinitionVersionV4() {
        Api api = new Api();
        api.setDefinitionVersion(DefinitionVersion.V4);
        api.setType(ApiType.MESSAGE);

        Plan plan = new Plan();
        plan.setId("plan-id");

        List<Flow> flows = List.of(new Flow());
        when(flowServiceV4.findByReferences(any(), anySet())).thenReturn(Map.of("plan-id", flows));

        genericPlanMapper.toGenericPlanWithFlow(api, plan);
        verify(planMapper).toEntity(plan, flows);
    }

    @Test
    public void shouldCallV4PlanMapperWhenDefinitionVersionNativeV4() {
        Api api = new Api();
        api.setDefinitionVersion(DefinitionVersion.V4);
        api.setType(ApiType.NATIVE);

        Plan plan = new Plan();
        plan.setId("plan-id");

        List<NativeFlow> nativeFlows = List.of(new NativeFlow());
        when(flowCrudService.getNativePlanFlows(anySet())).thenReturn(Map.of("plan-id", nativeFlows));

        genericPlanMapper.toGenericPlanWithFlow(api, plan);
        verify(planMapper).toNativeEntity(plan, nativeFlows);
    }

    @Test
    public void shouldCallV2ApiMapperWhenV2DefinitionVersion() {
        Api api = new Api();
        api.setDefinitionVersion(DefinitionVersion.V2);

        Plan plan = new Plan();
        plan.setId("plan-id");

        List<io.gravitee.definition.model.flow.Flow> v2Flows = List.of(new io.gravitee.definition.model.flow.Flow());
        when(flowCrudService.getPlanV2Flows(anySet())).thenReturn(Map.of("plan-id", v2Flows));

        genericPlanMapper.toGenericPlanWithFlow(api, plan);
        verify(planConverter).toPlanEntity(plan, v2Flows);
    }

    @Test
    public void shouldCallV2ApiMapperWhenV1DefinitionVersion() {
        Api api = new Api();
        api.setDefinitionVersion(DefinitionVersion.V1);

        Plan plan = new Plan();
        plan.setId("plan-id");

        List<io.gravitee.definition.model.flow.Flow> v2Flows = List.of(new io.gravitee.definition.model.flow.Flow());
        when(flowCrudService.getPlanV2Flows(anySet())).thenReturn(Map.of("plan-id", v2Flows));

        genericPlanMapper.toGenericPlanWithFlow(api, plan);
        verify(planConverter).toPlanEntity(plan, v2Flows);
    }

    @Test
    public void shouldCallV2ApiMapperWhenNoDefinitionVersion() {
        Api api = new Api();

        Plan plan = new Plan();
        plan.setId("plan-id");

        List<io.gravitee.definition.model.flow.Flow> v2Flows = List.of(new io.gravitee.definition.model.flow.Flow());
        when(flowCrudService.getPlanV2Flows(anySet())).thenReturn(Map.of("plan-id", v2Flows));

        genericPlanMapper.toGenericPlanWithFlow(api, plan);
        verify(planConverter).toPlanEntity(plan, v2Flows);
    }

    @Test
    public void shouldCallV4PlanMapperWithEmptyFlowsWhenDefinitionVersionV4AndNoFlowsFound() {
        Api api = new Api();
        api.setDefinitionVersion(DefinitionVersion.V4);
        api.setType(ApiType.MESSAGE);

        Plan plan = new Plan();
        plan.setId("plan-id");

        when(flowServiceV4.findByReferences(any(), anySet())).thenReturn(Map.of());

        genericPlanMapper.toGenericPlanWithFlow(api, plan);
        verify(planMapper).toEntity(plan, Collections.emptyList());
    }

    @Test
    public void shouldCallV4PlanMapperWithEmptyFlowsWhenDefinitionVersionNativeV4AndNoFlowsFound() {
        Api api = new Api();
        api.setDefinitionVersion(DefinitionVersion.V4);
        api.setType(ApiType.NATIVE);

        Plan plan = new Plan();
        plan.setId("plan-id");

        when(flowCrudService.getNativePlanFlows(anySet())).thenReturn(Map.of());

        genericPlanMapper.toGenericPlanWithFlow(api, plan);
        verify(planMapper).toNativeEntity(plan, Collections.emptyList());
    }

    @Test
    public void shouldCallV2ApiMapperWithEmptyFlowsWhenV2DefinitionVersionAndNoFlowsFound() {
        Api api = new Api();
        api.setDefinitionVersion(DefinitionVersion.V2);

        Plan plan = new Plan();
        plan.setId("plan-id");

        when(flowCrudService.getPlanV2Flows(anySet())).thenReturn(Map.of());

        genericPlanMapper.toGenericPlanWithFlow(api, plan);
        verify(planConverter).toPlanEntity(plan, Collections.emptyList());
    }
}
