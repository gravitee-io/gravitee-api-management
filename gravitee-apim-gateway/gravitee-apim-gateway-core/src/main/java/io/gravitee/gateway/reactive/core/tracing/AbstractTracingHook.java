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
package io.gravitee.gateway.reactive.core.tracing;

import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.hook.HttpHook;
import io.gravitee.gateway.reactive.api.tracing.Tracer;
import io.gravitee.node.api.opentelemetry.Span;
import io.gravitee.node.api.opentelemetry.internal.InternalRequest;
import io.reactivex.rxjava3.annotations.Nullable;
import io.reactivex.rxjava3.core.Completable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractTracingHook implements HttpHook {

    public static final String SPAN_PHASE_ATTR = "gravitee.execution.phase";
    public static final String ATTR_INTERNAL_TRACING_SPAN = "tracing-span-%s";

    @Override
    public Completable pre(final String id, final HttpExecutionContext ctx, final ExecutionPhase executionPhase) {
        return Completable.fromRunnable(() -> createSpan(id, ctx, executionPhase));
    }

    @Override
    public Completable post(final String id, final HttpExecutionContext ctx, final ExecutionPhase executionPhase) {
        return Completable.fromRunnable(() -> endSpan(id, ctx));
    }

    @Override
    public Completable error(
        final String id,
        final HttpExecutionContext ctx,
        final ExecutionPhase executionPhase,
        final Throwable throwable
    ) {
        return Completable.fromRunnable(() -> endSpanOnError(id, ctx, throwable));
    }

    @Override
    public Completable interrupt(final String id, final HttpExecutionContext ctx, final ExecutionPhase executionPhase) {
        return Completable.fromRunnable(() -> endSpan(id, ctx));
    }

    @Override
    public Completable interruptWith(
        final String id,
        final HttpExecutionContext ctx,
        final ExecutionPhase executionPhase,
        final ExecutionFailure failure
    ) {
        return Completable.fromRunnable(() -> endSpanWithFailure(id, ctx, failure));
    }

    @Override
    public void cancel(final String id, final HttpExecutionContext ctx, final @Nullable ExecutionPhase executionPhase) {
        endSpan(id, ctx);
    }

    protected void createSpan(final String id, final HttpExecutionContext ctx, final ExecutionPhase executionPhase) {
        Tracer tracer = ctx.getTracer();
        if (tracer != null) {
            Span span = tracer.startSpanFrom(
                InternalRequest.builder().name(spanName(id, executionPhase)).attributes(spanAttributes(id, ctx, executionPhase)).build()
            );
            putSpan(id, ctx, span);
        }
    }

    protected abstract String spanName(final String id, final ExecutionPhase executionPhase);

    protected Map<String, String> spanAttributes(final String id, final HttpExecutionContext ctx, final ExecutionPhase executionPhase) {
        Map<String, String> attributes = new HashMap<>();
        if (executionPhase != null) {
            attributes.put(SPAN_PHASE_ATTR, executionPhase.getLabel());
        }
        return attributes;
    }

    protected void endSpan(final String id, final HttpExecutionContext ctx) {
        Span span = getSpan(ctx, id);
        if (span != null) {
            Tracer tracer = ctx.getTracer();
            if (tracer != null) {
                tracer.end(span);
            }
            removeSpan(ctx, id);
        }
    }

    protected void endSpanOnError(final String id, final HttpExecutionContext ctx, final Throwable throwable) {
        Span span = getSpan(ctx, id);
        if (span != null) {
            Tracer tracer = ctx.getTracer();
            if (tracer != null) {
                tracer.endOnError(span, throwable);
            }
            removeSpan(ctx, id);
        }
    }

    protected void endSpanWithFailure(final String id, final HttpExecutionContext ctx, final ExecutionFailure failure) {
        Span span = getSpan(ctx, id);
        if (span != null) {
            Tracer tracer = ctx.getTracer();
            if (tracer != null) {
                span
                    .withAttribute("gravitee.execution-failure.key", failure.key())
                    .withAttribute("gravitee.execution-failure.status-code", failure.statusCode())
                    .withAttribute("gravitee.execution-failure.content-type", failure.contentType());
                tracer.endOnError(span, failure.message());
            }
            removeSpan(ctx, id);
        }
    }

    protected void putSpan(String id, HttpExecutionContext ctx, Span span) {
        ctx.putInternalAttribute(getCtxAttributeKey(id), span);
    }

    protected Span getSpan(HttpExecutionContext ctx, String id) {
        return ctx.getInternalAttribute(getCtxAttributeKey(id));
    }

    protected void removeSpan(HttpExecutionContext ctx, String id) {
        ctx.removeInternalAttribute(getCtxAttributeKey(id));
    }

    protected String getCtxAttributeKey(final String id) {
        return String.format(ATTR_INTERNAL_TRACING_SPAN, id);
    }
}
