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
package io.gravitee.gateway.security.core;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.core.processor.StreamableProcessor;
import io.gravitee.gateway.policy.*;
import io.gravitee.gateway.policy.impl.OrderedPolicyChain;
import io.gravitee.policy.api.PolicyResult;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SecurityPolicyChainProvider extends AbstractPolicyChainProvider {

    static final String PLAN_UNRESOLVABLE = "GATEWAY_PLAN_UNRESOLVABLE";

    private final PolicyResolver policyResolver;

    public SecurityPolicyChainProvider(final PolicyResolver policyResolver) {
        this.policyResolver = policyResolver;
    }

    @Override
    public StreamableProcessor<ExecutionContext, Buffer> provide(ExecutionContext context) {
        if (context.getAttribute("skip-security-chain") == null) {
            // Calculate the list of policies to apply under this policy chain
            List<Policy> policies = policyResolver.resolve(StreamType.ON_REQUEST, context);
            // TODO: setter un champs "place" sur les policies: SECURITY

            if (policies == null) {
                return new DirectPolicyChain(
                    PolicyResult.failure(PLAN_UNRESOLVABLE, HttpStatusCode.UNAUTHORIZED_401, "Unauthorized"),
                    context
                );
            }

            return OrderedPolicyChain.create(policies, context);
        }

        return new NoOpPolicyChain(context);
    }
}
