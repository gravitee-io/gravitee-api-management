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

/**
 * Gateway reactable for a first-class {@link io.gravitee.definition.model.v4.agent.AgentApi}, deployed through the
 * reused {@code DefaultApiReactor} via the <b>hybrid translation</b>: it extends the proxy {@link Api} reactable,
 * whose {@code definition} is a <i>synthetic</i> proxy {@code model.v4.Api} built by {@link AgentApiSynthesizer}
 * (so the shared pipeline sees a real proxy {@code Api}), while it also carries the clean agent definition for the
 * agent runtime to read via {@link #getAgentDefinition()}.
 *
 * @author GraviteeSource Team
 */
public class AgentApi extends Api {

    private final io.gravitee.definition.model.v4.agent.AgentApi agentDefinition;

    public AgentApi(final io.gravitee.definition.model.v4.agent.AgentApi agentDefinition) {
        super(AgentApiSynthesizer.synthesize(agentDefinition));
        this.agentDefinition = agentDefinition;
    }

    /** The first-class agent definition (model/instructions/tools/skills/memory or the workflow). */
    public io.gravitee.definition.model.v4.agent.AgentApi getAgentDefinition() {
        return agentDefinition;
    }
}
