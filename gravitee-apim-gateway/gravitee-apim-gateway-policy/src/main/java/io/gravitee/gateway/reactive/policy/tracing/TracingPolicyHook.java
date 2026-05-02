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

<<<<<<< HEAD
    public static final String SPAN_POLICY_ATTR = "policy";

    @Override
    public String id() {
        return "hook-tracing-policy";
=======
    private static final String HOOK_ID = "hook-tracing-policy";

    public static final String SPAN_POLICY_ATTR = "gravitee.policy";
    public static final String SPAN_POLICY_DESCRIPTION_ATTR = "gravitee.policy.description";
    public static final String ATTR_CURRENT_POLICY_DESCRIPTION = "tracing.policy.current-description";
    private static final String ATTR_SPAN_POLICY_TRIGGER_EXECUTED = "gravitee.policy.trigger.executed";
    private static final String ATTR_SPAN_POLICY_TRIGGER_CONDITION = "gravitee.policy.trigger.condition";
    public static final String ATTR_POLICY_TRIGGER_CONDITION_PREFIX = "gravitee.policy.trigger.condition.";
    public static final String ATTR_POLICY_TRIGGER_EXECUTED_PREFIX = "gravitee.policy.trigger.executed.";
    private static final String EVENT_POLICY_PRE = "gravitee.policy.pre";
    private static final String EVENT_POLICY_POST = "gravitee.policy.post";
    private static final String ATTR_HTTP_REQUEST_HEADER_PREFIX = "http.request.header.";
    private static final String ATTR_HTTP_RESPONSE_HEADER_PREFIX = "http.response.header.";

    @Override
    public String id() {
        return HOOK_ID;
    }

    @Override
    public Completable pre(final String id, final HttpExecutionContext ctx, final ExecutionPhase executionPhase) {
        return super
            .pre(id, ctx, executionPhase)
            .doOnComplete(() -> {
                if (isVerboseEnabled(ctx)) {
                    addPreExecutionEvent(id, ctx, executionPhase);
                }
            });
    }

    @Override
    public Completable post(final String id, final HttpExecutionContext ctx, final ExecutionPhase executionPhase) {
        return Completable.fromRunnable(() -> {
            addTriggerAttributes(id, ctx);

            if (isVerboseEnabled(ctx)) {
                addPostExecutionEvent(id, ctx, executionPhase);
            }
            endSpan(id, ctx);
        });
    }

    private void addTriggerAttributes(final String id, final HttpExecutionContext ctx) {
        Span span = getSpan(ctx, id);
        if (span != null) {
            String triggerCondition = ctx.getInternalAttribute(ATTR_POLICY_TRIGGER_CONDITION_PREFIX + id);
            if (triggerCondition != null && !triggerCondition.isBlank()) {
                span.withAttribute(ATTR_SPAN_POLICY_TRIGGER_CONDITION, triggerCondition);
            }
            Boolean executed = ctx.getInternalAttribute(ATTR_POLICY_TRIGGER_EXECUTED_PREFIX + id);
            span.withAttribute(ATTR_SPAN_POLICY_TRIGGER_EXECUTED, executed != null ? String.valueOf(executed) : "true");
        }
>>>>>>> 77f56fd870 (fix(tracing): report correct trigger.executed flag for policies in OTEL)
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
