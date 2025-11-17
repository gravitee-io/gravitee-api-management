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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TracingPolicyHook extends AbstractTracingHook implements PolicyHook {

    public static final String SPAN_POLICY_ATTR = "policy";
    private static final String PRE_EVENT_NAME = "policy.pre";
    private static final String POST_EVENT_NAME = "policy.post";
    private static final String ATTR_INTERNAL_POLICY_PRE_HEADERS = "policy.pre.headers.%s";
    private static final String ATTR_INTERNAL_POLICY_PRE_ATTRIBUTES = "policy.pre.attributes.%s";
    private static final String ATTR_POLICY_TRIGGER_CONDITION_PREFIX = "policy.trigger.condition.";

    @Override
    public String id() {
        return "hook-tracing-policy";
    }

    @Override
    public Completable pre(final String id, final HttpExecutionContext ctx, final ExecutionPhase executionPhase) {
        return super
            .pre(id, ctx, executionPhase)
            .doOnComplete(() -> {
                if (isVerboseEnabled(ctx)) {
                    addPreExecutionEvent(id, ctx);
                }
            });
    }

    @Override
    public Completable post(final String id, final HttpExecutionContext ctx, final ExecutionPhase executionPhase) {
        return Completable.fromRunnable(() -> {
            addTriggerAttributes(id, ctx);

            if (isVerboseEnabled(ctx)) {
                addPostExecutionEvent(id, ctx);
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
                span.withAttribute("policy.trigger.condition", triggerCondition);
            }
            span.withAttribute("policy.trigger.executed", "true");
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
        spanAttributes.put("policy.id", id);
        spanAttributes.put("policy.execution.phase", executionPhase != null ? executionPhase.getLabel() : "unknown");
        return spanAttributes;
    }

    private boolean isVerboseEnabled(final HttpExecutionContext ctx) {
        Boolean verbose = ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_TRACING_VERBOSE_ENABLED);
        return verbose != null && verbose;
    }

    private void addPreExecutionEvent(final String id, final HttpExecutionContext ctx) {
        Span span = getSpan(ctx, id);
        if (span != null) {
            Map<String, Object> eventAttributes = new HashMap<>();

            if (ctx.request() != null && ctx.request().headers() != null) {
                Map<String, String> headers = captureHeaders(ctx.request().headers().toSingleValueMap());
                ctx.setInternalAttribute(String.format(ATTR_INTERNAL_POLICY_PRE_HEADERS, id), headers);
                headers.forEach((key, value) -> eventAttributes.put("header." + key, value));
            }

            if (ctx.getAttributes() != null) {
                Map<String, String> attributes = captureContextAttributes(ctx);
                ctx.setInternalAttribute(String.format(ATTR_INTERNAL_POLICY_PRE_ATTRIBUTES, id), attributes);
                attributes.forEach((key, value) -> eventAttributes.put("context." + key, value));
            }

            span.addEvent(PRE_EVENT_NAME, eventAttributes);
        }
    }

    private void addPostExecutionEvent(final String id, final HttpExecutionContext ctx) {
        Span span = getSpan(ctx, id);
        if (span != null) {
            Map<String, Object> eventAttributes = new HashMap<>();

            Map<String, String> currentHeaders = null;
            if (ctx.request() != null && ctx.request().headers() != null) {
                currentHeaders = captureHeaders(ctx.request().headers().toSingleValueMap());
                currentHeaders.forEach((key, value) -> eventAttributes.put("header." + key, value));
            }

            Map<String, String> currentAttributes = null;
            if (ctx.getAttributes() != null) {
                currentAttributes = captureContextAttributes(ctx);
                currentAttributes.forEach((key, value) -> eventAttributes.put("context." + key, value));
            }

            detectAndAddChanges(id, ctx, currentHeaders, currentAttributes, eventAttributes);

            span.addEvent(POST_EVENT_NAME, eventAttributes);

            cleanupPreExecutionState(id, ctx);
        }
    }

    private void detectAndAddChanges(
        final String id,
        final HttpExecutionContext ctx,
        final Map<String, String> currentHeaders,
        final Map<String, String> currentAttributes,
        final Map<String, Object> eventAttributes
    ) {
        Map<String, String> preHeaders = ctx.getInternalAttribute(String.format(ATTR_INTERNAL_POLICY_PRE_HEADERS, id));
        if (preHeaders != null && currentHeaders != null) {
            detectHeaderChanges(preHeaders, currentHeaders, eventAttributes);
        }

        Map<String, String> preAttributes = ctx.getInternalAttribute(String.format(ATTR_INTERNAL_POLICY_PRE_ATTRIBUTES, id));
        if (preAttributes != null && currentAttributes != null) {
            detectAttributeChanges(preAttributes, currentAttributes, eventAttributes);
        }
    }

    private void cleanupPreExecutionState(final String id, final HttpExecutionContext ctx) {
        ctx.removeInternalAttribute(String.format(ATTR_INTERNAL_POLICY_PRE_HEADERS, id));
        ctx.removeInternalAttribute(String.format(ATTR_INTERNAL_POLICY_PRE_ATTRIBUTES, id));
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

    private void detectHeaderChanges(
        final Map<String, String> preHeaders,
        final Map<String, String> postHeaders,
        final Map<String, Object> eventAttributes
    ) {
        postHeaders.forEach((key, value) -> {
            if (!preHeaders.containsKey(key)) {
                eventAttributes.put("header.added." + key, value);
            } else if (!value.equals(preHeaders.get(key))) {
                eventAttributes.put("header.modified." + key, value);
                eventAttributes.put("header.modified." + key + ".previous", preHeaders.get(key));
            }
        });

        preHeaders.forEach((key, value) -> {
            if (!postHeaders.containsKey(key)) {
                eventAttributes.put("header.removed." + key, value);
            }
        });
    }

    private void detectAttributeChanges(
        final Map<String, String> preAttributes,
        final Map<String, String> postAttributes,
        final Map<String, Object> eventAttributes
    ) {
        postAttributes.forEach((key, value) -> {
            if (!preAttributes.containsKey(key)) {
                eventAttributes.put("context.added." + key, value);
            } else if (!value.equals(preAttributes.get(key))) {
                eventAttributes.put("context.modified." + key, value);
                eventAttributes.put("context.modified." + key + ".previous", preAttributes.get(key));
            }
        });

        preAttributes.forEach((key, value) -> {
            if (!postAttributes.containsKey(key)) {
                eventAttributes.put("context.removed." + key, value);
            }
        });
    }
}
