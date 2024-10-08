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
package io.gravitee.gateway.reactive.policy;

import io.gravitee.definition.model.flow.Flow;
import io.gravitee.gateway.reactive.api.ExecutionPhase;

/**
 * Allows creating {@link HttpPolicyChain}.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface PolicyChainFactory<P extends PolicyChain<?>> {
    /**
     * Creates a policy chain from the provided flow, for the given execution phase.
     * The policies composing the policy chain, depends on the specified execution phase:
     * <ul>
     *     <li>{@link ExecutionPhase#REQUEST}, {@link ExecutionPhase#MESSAGE_REQUEST}: {@link Flow#getPre()}</li>
     *     <li>{@link ExecutionPhase#RESPONSE}, {@link ExecutionPhase#MESSAGE_RESPONSE}: {@link Flow#getPost()}</li>
     * </ul>
     *
     * @param flowChainId the flow chain id in which one the policy chain will be executed
     * @param flow the flow where to extract the policies to create the policy chain.
     * @param phase the execution phase used to select the pre- or post-steps.
     *
     * @return the created {@link HttpPolicyChain}.
     */
    P create(String flowChainId, Flow flow, ExecutionPhase phase);
}
