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
package io.gravitee.gateway.handlers.api.definition;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.definition.model.Plan;
import io.gravitee.definition.model.Policy;
import io.gravitee.definition.model.flow.FlowEntity;
import io.gravitee.definition.model.flow.Step;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ApiTest {

    @Test
    public void shouldFilterDisabledFlowPreStep() {
        io.gravitee.definition.model.Api definition = new io.gravitee.definition.model.Api();
        Api api = new Api(definition);
        FlowEntity flow = new FlowEntity();
        flow.setPre(aStepList());
        definition.setFlows(List.of(flow));

        Set<Policy> result = api.dependencies(Policy.class);

        assertThat(result).hasSize(1).extracting(Policy::getName).containsExactly("enabledPolicy");
    }

    @Test
    public void shouldFilterDisabledFlowPostStep() {
        io.gravitee.definition.model.Api definition = new io.gravitee.definition.model.Api();
        Api api = new Api(definition);
        FlowEntity flow = new FlowEntity();
        flow.setPost(aStepList());
        definition.setFlows(List.of(flow));

        Set<Policy> result = api.dependencies(Policy.class);

        assertThat(result).hasSize(1).extracting(Policy::getName).containsExactly("enabledPolicy");
    }

    @Test
    public void shouldFilterDisabledPlanFlowPreStep() {
        io.gravitee.definition.model.Api definition = new io.gravitee.definition.model.Api();
        Api api = new Api(definition);
        FlowEntity flow = new FlowEntity();
        flow.setPre(aStepList());
        Plan plan = aPlan(List.of(flow));
        definition.setPlans(List.of(plan));

        Set<Policy> result = api.dependencies(Policy.class);

        assertThat(result).hasSize(2).extracting(Policy::getName).containsExactlyInAnyOrder("enabledPolicy", "key-less");
    }

    @Test
    public void shouldFilterDisabledPlanFlowPostStep() {
        io.gravitee.definition.model.Api definition = new io.gravitee.definition.model.Api();
        Api api = new Api(definition);
        FlowEntity flow = new FlowEntity();
        flow.setPost(aStepList());
        Plan plan = aPlan(List.of(flow));
        definition.setPlans(List.of(plan));

        Set<Policy> result = api.dependencies(Policy.class);

        assertThat(result).hasSize(2).extracting(Policy::getName).containsExactlyInAnyOrder("enabledPolicy", "key-less");
    }

    @Test
    public void shouldFilterDisabledFlows() {
        io.gravitee.definition.model.Api definition = new io.gravitee.definition.model.Api();
        Api api = new Api(definition);
        definition.setFlows(aFlowList());

        Set<Policy> result = api.dependencies(Policy.class);

        assertThat(result).hasSize(0);
    }

    @Test
    public void shouldFilterDisabledPlanFlows() {
        io.gravitee.definition.model.Api definition = new io.gravitee.definition.model.Api();
        Api api = new Api(definition);
        Plan plan = aPlan(aFlowList());
        definition.setPlans(List.of(plan));

        Set<Policy> result = api.dependencies(Policy.class);

        assertThat(result).hasSize(1).extracting(Policy::getName).containsExactlyInAnyOrder("key-less");
    }

    private List<Step> aStepList() {
        Step enabledPreStep = new Step();
        enabledPreStep.setPolicy("enabledPolicy");
        enabledPreStep.setEnabled(true);
        Step disabledPreStep = new Step();
        disabledPreStep.setName("disabledPolicy");
        disabledPreStep.setEnabled(false);
        return List.of(enabledPreStep, disabledPreStep);
    }

    private Plan aPlan(List<FlowEntity> flowList) {
        Plan plan = new Plan();
        plan.setPaths(null);
        plan.setSecurity("KEY_LESS");
        plan.setFlows(flowList);
        return plan;
    }

    private List<FlowEntity> aFlowList() {
        FlowEntity enabledFlow = new FlowEntity();
        enabledFlow.setEnabled(true);
        FlowEntity disabledFlow = new FlowEntity();
        disabledFlow.setPre(aStepList());
        disabledFlow.setEnabled(false);
        return List.of(enabledFlow, disabledFlow);
    }
}
