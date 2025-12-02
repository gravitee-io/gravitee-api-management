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
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.hook.PolicyHook;
import io.gravitee.gateway.reactive.core.tracing.AbstractTracingHook;
import io.gravitee.node.api.opentelemetry.Span;
import io.reactivex.rxjava3.core.Completable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TracingPolicyHook extends AbstractTracingHook implements PolicyHook {

    private static final String HOOK_ID = "hook-tracing-policy";

    public static final String SPAN_POLICY_ATTR = "gravitee.policy";
    private static final String ATTR_SPAN_POLICY_TRIGGER_EXECUTED = "gravitee.policy.trigger.executed";
    private static final String ATTR_SPAN_POLICY_TRIGGER_CONDITION = "gravitee.policy.trigger.condition";
    public static final String ATTR_POLICY_TRIGGER_CONDITION_PREFIX = "gravitee.policy.trigger.condition.";
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

    /**
     * Add policy.trigger.condition and policy.trigger.executed attributes to the span.
     * These must be added in post() because HttpConditionalPolicy stores the condition
     * in the context during policy execution, after the span is created.
     */
    private void addTriggerAttributes(final String id, final HttpExecutionContext ctx) {
        Span span = getSpan(ctx, id);
        if (span != null) {
            String triggerCondition = ctx.getInternalAttribute(ATTR_POLICY_TRIGGER_CONDITION_PREFIX + id);
            if (triggerCondition != null && !triggerCondition.isBlank()) {
                span.withAttribute(ATTR_SPAN_POLICY_TRIGGER_CONDITION, triggerCondition);
            }
            span.withAttribute(ATTR_SPAN_POLICY_TRIGGER_EXECUTED, "true");
        }
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

    private boolean isVerboseEnabled(final HttpExecutionContext ctx) {
        Boolean verbose = ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_TRACING_VERBOSE_ENABLED);
        return verbose != null && verbose;
    }

    private void addPreExecutionEvent(final String id, final HttpExecutionContext ctx, final ExecutionPhase executionPhase) {
        Span span = getSpan(ctx, id);
        if (span != null) {
            Map<String, Object> eventAttributes = captureEventAttributes(ctx, executionPhase);
            span.addEvent(EVENT_POLICY_PRE, eventAttributes);
        }
    }

    private void addPostExecutionEvent(final String id, final HttpExecutionContext ctx, final ExecutionPhase executionPhase) {
        Span span = getSpan(ctx, id);
        if (span != null) {
            Map<String, Object> eventAttributes = captureEventAttributes(ctx, executionPhase);
            span.addEvent(EVENT_POLICY_POST, eventAttributes);
        }
    }

    private Map<String, Object> captureEventAttributes(final HttpExecutionContext ctx, final ExecutionPhase executionPhase) {
        Map<String, Object> eventAttributes = new LinkedHashMap<>();

        if (executionPhase == ExecutionPhase.REQUEST && ctx.request() != null && ctx.request().headers() != null) {
            Map<String, String> headers = captureHeaders(ctx.request().headers().toSingleValueMap());
            headers.forEach((key, value) -> eventAttributes.put(ATTR_HTTP_REQUEST_HEADER_PREFIX + key, value));
        } else if (executionPhase == ExecutionPhase.RESPONSE && ctx.response() != null && ctx.response().headers() != null) {
            Map<String, String> headers = captureHeaders(ctx.response().headers().toSingleValueMap());
            headers.forEach((key, value) -> eventAttributes.put(ATTR_HTTP_RESPONSE_HEADER_PREFIX + key, value));
        }

        if (ctx.getAttributes() != null) {
            Map<String, String> attributes = captureContextAttributes(ctx);
            attributes.forEach(eventAttributes::put);
        }

        return eventAttributes;
    }

    Map<String, String> captureHeaders(final Map<String, String> headers) {
        Map<String, String> captured = new LinkedHashMap<>();
        if (headers == null) {
            return captured;
        }

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();

            if (value != null) {
                captured.put(name, value);
            }
        }
        return captured;
    }

    Map<String, String> captureContextAttributes(final HttpExecutionContext ctx) {
        Map<String, String> captured = new LinkedHashMap<>();
        Set<String> attributeNames = ctx.getAttributeNames();
        if (attributeNames == null) {
            return captured;
        }

        for (String name : attributeNames) {
            Object value = ctx.getAttribute(name);
            if (value != null) {
                captured.put(name, String.valueOf(value));
            }
        }

        return captured;
    }
}
