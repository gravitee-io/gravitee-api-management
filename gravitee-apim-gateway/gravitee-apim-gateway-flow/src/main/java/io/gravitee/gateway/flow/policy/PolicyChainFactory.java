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
package io.gravitee.gateway.flow.policy;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.core.processor.StreamableProcessor;
import io.gravitee.gateway.policy.NoOpPolicyChain;
import io.gravitee.gateway.policy.Policy;
import io.gravitee.gateway.policy.PolicyManager;
import io.gravitee.gateway.policy.StreamType;
import io.gravitee.gateway.policy.impl.OrderedPolicyChain;
import io.gravitee.gateway.policy.impl.ReversedPolicyChain;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A factory of {@link io.gravitee.policy.api.PolicyChain}.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PolicyChainFactory {

    private final PolicyManager policyManager;

    public PolicyChainFactory(final PolicyManager policyManager) {
        this.policyManager = policyManager;
    }

    public StreamableProcessor<ExecutionContext, Buffer> create(
        final List<PolicyResolver.Policy> resolvedPolicies,
        final StreamType streamType,
        final ExecutionContext context
    ) {
        return create(
            resolvedPolicies,
            streamType,
            context,
            policies ->
                streamType == StreamType.ON_REQUEST
                    ? OrderedPolicyChain.create(policies, context)
                    : ReversedPolicyChain.create(policies, context)
        );
    }

    public StreamableProcessor<ExecutionContext, Buffer> create(
        List<PolicyResolver.Policy> resolvedPolicies,
        StreamType streamType,
        ExecutionContext context,
        Function<List<Policy>, StreamableProcessor<ExecutionContext, Buffer>> mapper
    ) {
        if (resolvedPolicies.isEmpty()) {
            return new NoOpPolicyChain(context);
        }

        final List<Policy> policies = resolvedPolicies
            .stream()
            .map(policy -> policyManager.create(streamType, policy.getName(), policy.getConfiguration(), policy.getCondition()))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        return mapper.apply(policies);
    }
}
