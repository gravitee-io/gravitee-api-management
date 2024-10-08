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
import io.gravitee.tracing.api.Span;
import io.gravitee.tracing.api.Tracer;
import io.reactivex.rxjava3.core.Completable;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractTracingHook implements HttpHook {

    private static final String SPAN_PHASE_ATTR = "execution.phase";
    private static final String ATTR_INTERNAL_TRACING_SPAN = "tracing-span-%s";

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

    protected void createSpan(final String id, final HttpExecutionContext ctx, final ExecutionPhase executionPhase) {
        Tracer tracer = ctx.getComponent(Tracer.class);
        if (tracer != null) {
            Span span = tracer.span(getSpanName(id, executionPhase));
            withAttributes(id, ctx, executionPhase, span);
            putSpan(id, ctx, span);
        }
    }

    protected abstract String getSpanName(final String id, final ExecutionPhase executionPhase);

    protected void withAttributes(final String id, final HttpExecutionContext ctx, final ExecutionPhase executionPhase, final Span span) {
        if (executionPhase != null) {
            span.withAttribute(SPAN_PHASE_ATTR, executionPhase.getLabel());
        }
    }

    protected void endSpan(final String id, final HttpExecutionContext ctx) {
        Span span = getSpan(ctx, id);
        if (span != null) {
            span.end();
            removeSpan(ctx, id);
        }
    }

    protected void endSpanOnError(final String is, final HttpExecutionContext ctx, final Throwable throwable) {
        Span span = getSpan(ctx, is);
        if (span != null) {
            span.reportError(throwable).end();
            removeSpan(ctx, is);
        }
    }

    protected void endSpanWithFailure(final String id, final HttpExecutionContext ctx, final ExecutionFailure failure) {
        Span span = getSpan(ctx, id);
        if (span != null) {
            span
                .withAttribute("failure.key", failure.key())
                .withAttribute("failure.status-code", failure.statusCode())
                .withAttribute("failure.content-type", failure.contentType())
                .reportError(failure.message())
                .end();
            removeSpan(ctx, id);
        }
    }

    private void putSpan(String id, HttpExecutionContext ctx, Span span) {
        ctx.putInternalAttribute(getCtxAttributeKey(id), span);
    }

    private Span getSpan(HttpExecutionContext ctx, String is) {
        return ctx.getInternalAttribute(getCtxAttributeKey(is));
    }

    private void removeSpan(HttpExecutionContext ctx, String id) {
        ctx.removeInternalAttribute(getCtxAttributeKey(id));
    }

    private String getCtxAttributeKey(final String id) {
        return String.format(ATTR_INTERNAL_TRACING_SPAN, id);
    }
}
