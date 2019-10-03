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
import io.gravitee.gateway.policy.*;
import io.gravitee.gateway.policy.impl.RequestPolicyChain;
import io.gravitee.gateway.policy.impl.ResponsePolicyChain;
import io.gravitee.policy.api.PolicyResult;

import java.util.List;

/**
 * A policy chain provider based on the policy configuration at the plan level.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PlanPolicyChainProvider extends AbstractPolicyChainProvider {

    private static final String GATEWAY_MISSING_SECURED_REQUEST_PLAN_KEY = "GATEWAY_MISSING_SECURED_REQUEST_PLAN";

    private final StreamType streamType;

    public PlanPolicyChainProvider(final StreamType streamType, final PolicyResolver policyResolver) {
        super(policyResolver);
        this.streamType = streamType;
    }

    @Override
    public StreamableProcessor<ExecutionContext, Buffer> provide(ExecutionContext context) {
        // Store information about the resolved plan (according to the incoming request)
        String plan = (String) context.getAttribute(ExecutionContext.ATTR_PLAN);
        String application = (String) context.getAttribute(ExecutionContext.ATTR_APPLICATION);

        context.request().metrics().setPlan(plan);
        context.request().metrics().setApplication(application);
        context.request().metrics().setSubscription((String) context.getAttribute(ExecutionContext.ATTR_SUBSCRIPTION_ID));

        // Calculate the list of policies to apply under this policy chain
        List<Policy> policies = policyResolver.resolve(streamType, context);

        // No policies has been calculated on the ON_REQUEST phase
        // Returning a 401 because no plan is associated to the incoming secured request
        if (streamType == StreamType.ON_REQUEST && policies == null) {
            return new DirectPolicyChain(
                    PolicyResult.failure(
                            GATEWAY_MISSING_SECURED_REQUEST_PLAN_KEY,
                            HttpStatusCode.UNAUTHORIZED_401,
                            "Unauthorized"), context);
        } else if (policies.isEmpty()) {
            return new NoOpPolicyChain(context);
        }

        return (streamType == StreamType.ON_REQUEST) ?
                RequestPolicyChain.create(policies, context) :
                ResponsePolicyChain.create(policies, context);
    }
}
