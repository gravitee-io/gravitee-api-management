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
package io.gravitee.gateway.handlers.api.processor.policy.plan;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.core.processor.StreamableProcessor;
import io.gravitee.gateway.policy.AbstractPolicyChainProvider;
import io.gravitee.gateway.policy.NoOpPolicyChain;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class PlanProcessorProvider extends AbstractPolicyChainProvider {

    private static final String APPLICATION_NAME_ANONYMOUS = "1";
    private static final String PLAN_NAME_ANONYMOUS = "1";

    @Override
    public StreamableProcessor<ExecutionContext, Buffer> provide(ExecutionContext context) {
        if (context.getAttribute("skip-security-chain") == null) {
            // Store information about the resolved plan (according to the incoming request)
            String plan = (String) context.getAttribute(ExecutionContext.ATTR_PLAN);
            String application = (String) context.getAttribute(ExecutionContext.ATTR_APPLICATION);

            context.request().metrics().setPlan(plan);
            context.request().metrics().setApplication(application);
            context.request().metrics().setSubscription((String) context.getAttribute(ExecutionContext.ATTR_SUBSCRIPTION_ID));

            return provide0(context);
        } else {
            // Fix consuming application and subscription which are data that can be used by policies (ie. rate-limit).
            context.setAttribute(ExecutionContext.ATTR_APPLICATION, APPLICATION_NAME_ANONYMOUS);
            context.setAttribute(ExecutionContext.ATTR_PLAN, PLAN_NAME_ANONYMOUS);
            context.setAttribute(ExecutionContext.ATTR_SUBSCRIPTION_ID, context.request().remoteAddress());

            context.request().metrics().setApplication(APPLICATION_NAME_ANONYMOUS);
            context.request().metrics().setPlan(PLAN_NAME_ANONYMOUS);
            context.request().metrics().setSubscription((String) context.getAttribute(ExecutionContext.ATTR_SUBSCRIPTION_ID));

            return new NoOpPolicyChain(context);
        }
    }

    protected abstract StreamableProcessor<ExecutionContext, Buffer> provide0(ExecutionContext context);
}
