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
package io.gravitee.gateway.handlers.api.policy.plan;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.core.processor.StreamableProcessor;
import io.gravitee.gateway.flow.policy.PolicyChainFactory;
import io.gravitee.gateway.flow.policy.PolicyResolver;
import io.gravitee.gateway.handlers.api.processor.policy.plan.PlanProcessorProvider;
import io.gravitee.gateway.policy.DirectPolicyChain;
import io.gravitee.gateway.policy.PolicyMetadata;
import io.gravitee.gateway.policy.StreamType;
import io.gravitee.policy.api.PolicyResult;
import java.util.List;

/**
 * A policy chain provider based on the policy configuration at the plan level.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PlanPolicyChainProvider extends PlanProcessorProvider {

    private static final String GATEWAY_MISSING_SECURED_REQUEST_PLAN_KEY = "GATEWAY_MISSING_SECURED_REQUEST_PLAN";

    private final StreamType streamType;

    private final PolicyChainFactory policyChainFactory;

    private final PolicyResolver policyResolver;

    public PlanPolicyChainProvider(
        final StreamType streamType,
        final PolicyResolver policyResolver,
        final PolicyChainFactory policyChainFactory
    ) {
        this.policyResolver = policyResolver;
        this.streamType = streamType;
        this.policyChainFactory = policyChainFactory;
    }

    @Override
    protected StreamableProcessor<ExecutionContext, Buffer> provide0(ExecutionContext context) {
        // Calculate the list of policies to apply under this policy chain
        List<PolicyMetadata> policies = policyResolver.resolve(streamType, context);

        // No policies has been calculated on the ON_REQUEST phase
        // Returning a 401 because no plan is associated to the incoming secured request
        //TODO: is this still relevant ? What is the use-case
        if (streamType == StreamType.ON_REQUEST && policies == null) {
            return new DirectPolicyChain(
                PolicyResult.failure(GATEWAY_MISSING_SECURED_REQUEST_PLAN_KEY, HttpStatusCode.UNAUTHORIZED_401, "Unauthorized"),
                context
            );
        }

        return policyChainFactory.create(policies, streamType, context);
    }
}
