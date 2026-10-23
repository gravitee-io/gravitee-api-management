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
import io.gravitee.gateway.reactive.policy.action.PolicyActionType;
import java.util.Map;

/**
 * Traces an action a policy registered on the execution context, in a span of its own named after the policy and the
 * action type (e.g. {@code REQUEST policy-guardian (endpoint-request action)}). It carries the same attributes and
 * verbose events as the policy spans, plus {@value #SPAN_POLICY_ACTION_ATTR}.
 */
public class TracingPolicyActionHook extends TracingPolicyHook {

    public static final String SPAN_POLICY_ACTION_ATTR = "gravitee.policy.action";

    private static final String HOOK_ID = "hook-tracing-policy-action";

    private final PolicyActionType type;

    public TracingPolicyActionHook(final PolicyActionType type) {
        this.type = type;
    }

    @Override
    public String id() {
        return HOOK_ID;
    }

    @Override
    protected String spanName(final String id, final ExecutionPhase executionPhase) {
        return "%s (%s action)".formatted(super.spanName(id, executionPhase), type.label());
    }

    @Override
    protected Map<String, String> spanAttributes(final String id, final HttpExecutionContext ctx, final ExecutionPhase executionPhase) {
        Map<String, String> spanAttributes = super.spanAttributes(id, ctx, executionPhase);
        // The current policy description is the one of the last policy executed by a chain, not of this action.
        spanAttributes.remove(SPAN_POLICY_DESCRIPTION_ATTR);
        spanAttributes.put(SPAN_POLICY_ACTION_ATTR, type.label());
        return spanAttributes;
    }

    @Override
    protected void addTriggerAttributes(final String id, final HttpExecutionContext ctx) {
        // An action has no trigger condition: it runs once registered.
    }

    @Override
    protected String getCtxAttributeKey(final String id) {
        // Kept apart from the span of the policy execution itself, which uses the policy id alone.
        return super.getCtxAttributeKey("%s-%s-action".formatted(id, type.label()));
    }
}
