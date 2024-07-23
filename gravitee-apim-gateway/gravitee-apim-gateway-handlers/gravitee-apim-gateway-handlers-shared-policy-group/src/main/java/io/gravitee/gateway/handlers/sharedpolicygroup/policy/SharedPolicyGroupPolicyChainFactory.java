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
package io.gravitee.gateway.handlers.sharedpolicygroup.policy;

import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.policy.PolicyChain;
import io.gravitee.gateway.reactive.policy.PolicyChainFactory;
import java.util.List;

public interface SharedPolicyGroupPolicyChainFactory extends PolicyChainFactory {
    @Override
    default PolicyChain create(String flowChainId, Flow flow, ExecutionPhase phase) {
        throw new IllegalArgumentException("Cannot build a policy chain from a Flow for Shared Policy Group");
    }

    PolicyChain create(final String sharedPolicyGroupPolicyId, String environmentId, List<Step> steps, ExecutionPhase phase);
}
