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
package io.gravitee.apim.gateway.tests.sdk.converters;

import io.gravitee.definition.model.v4.agent.AgentApi;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.gateway.reactor.ReactableApi;
import java.util.Collections;
import org.junit.platform.commons.PreconditionViolationException;

/**
 * Deploys a {@link AgentApi} definition in an integration test.
 *
 * <p>An agent is not a V4 API that happens to talk to a model: the definition carries the model, tools, skills and
 * memory, and the HTTP API it is served as is <em>derived</em> from those by {@code AgentApiSynthesizer} inside the
 * reactable's constructor. That is why this returns the agent reactable rather than building an
 * {@code io.gravitee.gateway.reactive.handlers.api.v4.Api} — the agent handler claims the reactable by type, and a
 * plain V4 wrapper would be routed to the proxy handler with everything agent-specific dropped.</p>
 *
 * <p>The raw generic is deliberate. {@link ApiDeploymentPreparer} pairs a definition with a
 * {@code ReactableApi} of that same definition, which holds for every other type here; an agent's reactable is
 * parameterised by the <em>synthesized</em> V4 definition instead, so the two cannot be stated as one type
 * parameter.</p>
 */
public class AgentApiDeploymentPreparer implements ApiDeploymentPreparer<AgentApi> {

    @Override
    @SuppressWarnings("unchecked")
    public ReactableApi toReactable(AgentApi definition, String environmentId) {
        final io.gravitee.gateway.reactive.handlers.api.v4.AgentApi agent = new io.gravitee.gateway.reactive.handlers.api.v4.AgentApi(
            definition
        );
        agent.setEnvironmentId(environmentId);
        return agent;
    }

    @Override
    public void ensureMinimalRequirementForApi(AgentApi definition) {
        if (definition.getType() == null) {
            throw new PreconditionViolationException("'type' field must be defined on an Agent API Definition");
        }
        this.addDefaultKeylessPlanIfNeeded(definition);
    }

    /**
     * A test definition rarely bothers with a plan, and the deploy gate requires a published one. Note the synthesizer
     * also attaches a synthetic plan, but only for a composable agent with none — an agent meant to be called over
     * HTTP still needs one of its own, which is what this supplies.
     */
    protected void addDefaultKeylessPlanIfNeeded(AgentApi api) {
        if (api.getPlans() == null || api.getPlans().isEmpty()) {
            Plan plan = new Plan();
            plan.setId("default_plan");
            plan.setName("Default plan");
            final PlanSecurity planSecurity = new PlanSecurity();
            planSecurity.setType("key-less");
            plan.setSecurity(planSecurity);
            plan.setMode(PlanMode.STANDARD);
            plan.setStatus(PlanStatus.PUBLISHED);

            api.setPlans(Collections.singletonList(plan));
        } else {
            api
                .getPlans()
                .stream()
                .filter(plan -> plan.getStatus() == null)
                .forEach(plan -> plan.setStatus(PlanStatus.PUBLISHED));
        }
    }
}
