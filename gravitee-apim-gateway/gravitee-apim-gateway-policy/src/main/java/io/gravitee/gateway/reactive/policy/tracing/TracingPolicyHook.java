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

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.hook.PolicyHook;
import io.gravitee.gateway.reactive.core.tracing.AbstractTracingHook;
import io.gravitee.gateway.reactive.core.v4.analytics.AnalyticsContext;
import io.gravitee.gateway.reactive.core.v4.analytics.LoggingContext;
import io.gravitee.node.api.opentelemetry.Span;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
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
    static final String ATTR_HTTP_REQUEST_BODY = "http.request.body";
    static final String ATTR_HTTP_RESPONSE_BODY = "http.response.body";
    /** Marks a body identical to the one captured on the previous event. */
    static final String ATTR_HTTP_BODY_UNCHANGED = "http.body.unchanged";
    /** Marks a body cut to the configured maximum log size. */
    static final String ATTR_HTTP_BODY_TRUNCATED = "http.body.truncated";
    /** Per-request memory of the last captured body, so unchanged bodies are not re-sent. */
    private static final String ATTR_INTERNAL_LAST_BODY_HASH = "tracing.policy.last-body-hash.";

    @Override
    public String id() {
        return HOOK_ID;
    }

    @Override
    public Completable pre(final String id, final HttpExecutionContext ctx, final ExecutionPhase executionPhase) {
        return super
            .pre(id, ctx, executionPhase)
            .andThen(Completable.defer(() -> addExecutionEvent(id, ctx, executionPhase, EVENT_POLICY_PRE)));
    }

    @Override
    public Completable post(final String id, final HttpExecutionContext ctx, final ExecutionPhase executionPhase) {
        return Completable.fromRunnable(() -> addTriggerAttributes(id, ctx))
            .andThen(Completable.defer(() -> addExecutionEvent(id, ctx, executionPhase, EVENT_POLICY_POST)))
            // The span must outlive its events.
            .andThen(Completable.fromRunnable(() -> endSpan(id, ctx)));
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
        if (isVerboseEnabled(ctx)) {
            String description = ctx.getInternalAttribute(ATTR_CURRENT_POLICY_DESCRIPTION);
            if (description != null && !description.isBlank()) {
                spanAttributes.put(SPAN_POLICY_DESCRIPTION_ATTR, description);
            }
        }
        return spanAttributes;
    }

    private boolean isVerboseEnabled(final HttpExecutionContext ctx) {
        Boolean verbose = ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_TRACING_VERBOSE_ENABLED);
        return verbose != null && verbose;
    }

    /**
     * Records what the request or response looked like around one policy.
     *
     * Headers and context attributes are read synchronously; the body is only
     * materialized when body capture is on, which is why this returns a
     * {@link Completable} rather than running inline.
     */
    private Completable addExecutionEvent(
        final String id,
        final HttpExecutionContext ctx,
        final ExecutionPhase executionPhase,
        final String eventName
    ) {
        if (!isVerboseEnabled(ctx)) {
            return Completable.complete();
        }
        final Span span = getSpan(ctx, id);
        if (span == null) {
            return Completable.complete();
        }

        final Map<String, Object> eventAttributes = captureEventAttributes(ctx, executionPhase);

        return captureBody(ctx, executionPhase, eventAttributes).doOnComplete(() -> span.addEvent(eventName, eventAttributes));
    }

    /**
     * Adds the message body to the event when body capture applies.
     *
     * Deliberately conservative: it only reads a body the gateway is already
     * buffering for payload logging, streamed bodies yield nothing, and a body
     * identical to the previously captured one is reported as unchanged instead
     * of being sent again — a chain of ten policies where one transforms the
     * payload then carries the payload once, not ten times.
     */
    private Completable captureBody(
        final HttpExecutionContext ctx,
        final ExecutionPhase executionPhase,
        final Map<String, Object> eventAttributes
    ) {
        final LoggingContext loggingContext = loggingContext(ctx);
        if (loggingContext == null) {
            return Completable.complete();
        }

        final boolean isRequest = executionPhase == ExecutionPhase.REQUEST;
        if (isRequest ? !loggingContext.entrypointRequestPayload() : !loggingContext.entrypointResponsePayload()) {
            return Completable.complete();
        }

        final Maybe<Buffer> body = isRequest
            ? (ctx.request() != null ? ctx.request().body() : Maybe.empty())
            : (ctx.response() != null ? ctx.response().body() : Maybe.empty());

        final String bodyKey = isRequest ? ATTR_HTTP_REQUEST_BODY : ATTR_HTTP_RESPONSE_BODY;
        final String hashKey = ATTR_INTERNAL_LAST_BODY_HASH + executionPhase;
        final int maxSize = loggingContext.getMaxSizeLogMessage();

        return body
            .doOnSuccess(buffer -> {
                final String content = buffer == null ? "" : buffer.toString();
                final Integer previousHash = ctx.getInternalAttribute(hashKey);
                final int hash = content.hashCode();

                if (previousHash != null && previousHash == hash) {
                    eventAttributes.put(ATTR_HTTP_BODY_UNCHANGED, "true");
                    return;
                }
                ctx.setInternalAttribute(hashKey, hash);

                if (maxSize > 0 && content.length() > maxSize) {
                    eventAttributes.put(bodyKey, content.substring(0, maxSize));
                    eventAttributes.put(ATTR_HTTP_BODY_TRUNCATED, "true");
                } else {
                    eventAttributes.put(bodyKey, content);
                }
            })
            .ignoreElement()
            // Capturing the body must never break the request it observes.
            .onErrorComplete();
    }

    private LoggingContext loggingContext(final HttpExecutionContext ctx) {
        final AnalyticsContext analyticsContext = ctx.getInternalAttribute(
            io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_ANALYTICS_CONTEXT
        );
        return analyticsContext == null ? null : analyticsContext.getLoggingContext();
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
