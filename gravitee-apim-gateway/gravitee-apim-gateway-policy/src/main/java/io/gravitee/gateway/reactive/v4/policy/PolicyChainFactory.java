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
package io.gravitee.gateway.reactive.v4.policy;

import io.gravitee.definition.model.v4.flow.AbstractFlow;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.policy.HttpPolicyChain;
import io.gravitee.gateway.reactive.policy.PolicyChain;

/**
 * Allows creating {@link HttpPolicyChain}.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface PolicyChainFactory<P extends PolicyChain<?>, F extends AbstractFlow> {
    /**
     * Creates a policy chain from the provided flow, for the given execution phase.
     * The policies composing the policy chain, depends on the specified execution phase.
     *
     * @param flowChainId the flow chain id in which one the policy chain will be executed
     * @param flow the flow where to extract the policies to create the policy chain.
     * @param phase the execution phase used to select the relevant steps list in the flow.
     *
     * @return the created {@link PolicyChain}.
     */
    P create(String flowChainId, F flow, ExecutionPhase phase);
}
