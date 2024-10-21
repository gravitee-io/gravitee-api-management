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
package io.gravitee.gateway.reactive.policy.tracing;

import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.hook.PolicyHook;
import io.gravitee.gateway.reactive.core.tracing.AbstractTracingHook;
import java.util.Map;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TracingPolicyHook extends AbstractTracingHook implements PolicyHook {

    public static final String SPAN_POLICY_ATTR = "policy";

    @Override
    public String id() {
        return "hook-tracing-policy";
    }

    @Override
    protected String spanName(final String id, final ExecutionPhase executionPhase) {
        StringBuilder spanNameBuilder = new StringBuilder();
        if (executionPhase != null) {
            spanNameBuilder.append(executionPhase.name()).append(" ");
        }
        if (!id.startsWith("policy-")) {
            spanNameBuilder.append("policy-");
        }
        spanNameBuilder.append(id);
        return spanNameBuilder.toString();
    }

    @Override
    protected Map<String, String> spanAttributes(final String id, final HttpExecutionContext ctx, final ExecutionPhase executionPhase) {
        Map<String, String> spanAttributes = super.spanAttributes(id, ctx, executionPhase);
        spanAttributes.put(SPAN_POLICY_ATTR, id);
        return spanAttributes;
    }
}
