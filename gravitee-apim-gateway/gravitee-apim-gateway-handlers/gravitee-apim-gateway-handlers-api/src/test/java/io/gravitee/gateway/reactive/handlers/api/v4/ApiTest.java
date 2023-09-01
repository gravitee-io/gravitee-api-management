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
package io.gravitee.gateway.reactive.handlers.api.v4;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.definition.model.Policy;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
class ApiTest {

    @ParameterizedTest
    @CsvSource({ "API_KEY,STANDARD", "api-key,STANDARD", "JWT,STANDARD", "OAUTH2,STANDARD", ",PUSH" })
    void shouldReturnSubscribablePlans(String securityType, String mode) {
        final io.gravitee.definition.model.v4.Api definition = new io.gravitee.definition.model.v4.Api();
        final ArrayList<Plan> plans = new ArrayList<>();
        final PlanSecurity planSecurity = new PlanSecurity();
        final Plan plan = new Plan();
        plan.setMode(PlanMode.valueOf(mode));

        if (plan.getMode() != PlanMode.PUSH) {
            planSecurity.setType(securityType);
            plan.setSecurity(planSecurity);
        }
        plan.setId("subscribable");
        plans.add(plan);

        for (int i = 0; i < 5; i++) {
            final Plan other = new Plan();
            final PlanSecurity otherSecurity = new PlanSecurity();
            other.setMode(PlanMode.STANDARD);
            other.setId("not-subscribable-" + i);
            other.setSecurity(otherSecurity);
            plans.add(other);
        }

        definition.setPlans(plans);

        final Api api = new Api(definition);

        assertThat(api.getSubscribablePlans()).containsExactly("subscribable");
    }

    @ParameterizedTest
    @CsvSource({ "API_KEY,STANDARD", "api-key,STANDARD", "JWT,STANDARD", "OAUTH2,STANDARD", ",PUSH" })
    void should_return_dependencies_from_flow_policies(String securityType, String mode) {
        final io.gravitee.definition.model.v4.Api definition = new io.gravitee.definition.model.v4.Api();
        definition.setPlans(List.of(aPlan("plan-" + securityType, securityType, mode)));
        definition.setFlows(aFlowList("api-flow"));

        final Api api = new Api(definition);
        final Set<Policy> dependencies = api.dependencies(Policy.class);

        // We expect to have
        //  - 1 policy per phase (request, response, publish, subscribe) on plan flows -> 4
        //  - 1 policy per phase on api flows -> 4
        //  - 1 security policy -> 1
        // Total 9 policies

        int expectedPolicyCount = 9;
        if (mode.equals("PUSH")) {
            // Push plan doesn't have security policy -> 8 policies expected.
            expectedPolicyCount = 8;
        }

        assertThat(dependencies).hasSize(expectedPolicyCount);

        final List<String> expectedPolicies = new ArrayList<>(
            List.of(
                "plan-" + securityType + "-flow-request",
                "plan-" + securityType + "-flow-response",
                "plan-" + securityType + "-flow-publish",
                "plan-" + securityType + "-flow-subscribe",
                "api-flow-request",
                "api-flow-response",
                "api-flow-publish",
                "api-flow-subscribe"
            )
        );

        if (!mode.equals("PUSH")) {
            expectedPolicies.add(securityType);
        }

        assertThat(dependencies.stream().map(Policy::getName)).containsExactlyInAnyOrderElementsOf(expectedPolicies);
    }

    private Plan aPlan(String name, String securityType, String mode) {
        Plan plan = new Plan();
        plan.setName(name);
        plan.setSecurity(new PlanSecurity(securityType, "{}"));
        plan.setMode(PlanMode.valueOf(mode));
        plan.setFlows(aFlowList(name + "-flow"));
        return plan;
    }

    private List<Flow> aFlowList(String name) {
        Flow enabledFlow = new Flow();
        enabledFlow.setName(name);
        enabledFlow.setEnabled(true);
        enabledFlow.setRequest(aStepList(name + "-request"));
        enabledFlow.setResponse(aStepList(name + "-response"));
        enabledFlow.setPublish(aStepList(name + "-publish"));
        enabledFlow.setSubscribe(aStepList(name + "-subscribe"));

        Flow disabledFlow = new Flow();
        disabledFlow.setRequest(aStepList("disabled-" + name + "-request"));
        disabledFlow.setResponse(aStepList("disabled-" + name + "-response"));
        disabledFlow.setPublish(aStepList("disabled-" + name + "-publish"));
        disabledFlow.setSubscribe(aStepList("disabled-" + name + "-subscribe"));

        disabledFlow.setEnabled(false);
        disabledFlow.setName("disabled-" + name);
        disabledFlow.setRequest(aStepList(name + "-request"));
        disabledFlow.setResponse(aStepList(name + "-response"));
        disabledFlow.setPublish(aStepList(name + "-publish"));
        disabledFlow.setSubscribe(aStepList(name + "-subscribe"));
        return List.of(enabledFlow, disabledFlow);
    }

    private List<Step> aStepList(String policy) {
        Step enabledPreStep = new Step();
        enabledPreStep.setPolicy(policy);
        enabledPreStep.setEnabled(true);
        Step disabledPreStep = new Step();
        disabledPreStep.setName("disabled-" + policy);
        disabledPreStep.setEnabled(false);
        return List.of(enabledPreStep, disabledPreStep);
    }
}
