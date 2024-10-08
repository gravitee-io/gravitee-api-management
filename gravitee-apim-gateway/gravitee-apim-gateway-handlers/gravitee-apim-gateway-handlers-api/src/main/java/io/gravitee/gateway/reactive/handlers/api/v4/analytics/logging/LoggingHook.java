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
package io.gravitee.gateway.reactive.handlers.api.v4.analytics.logging;

import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.hook.InvokerHook;
import io.gravitee.gateway.reactive.core.context.HttpExecutionContextInternal;
import io.gravitee.gateway.reactive.core.context.HttpResponseInternal;
import io.gravitee.gateway.reactive.core.v4.analytics.AnalyticsContext;
import io.gravitee.gateway.reactive.core.v4.analytics.LoggingContext;
import io.gravitee.gateway.reactive.handlers.api.v4.analytics.logging.request.LogEndpointRequest;
import io.gravitee.gateway.reactive.handlers.api.v4.analytics.logging.response.LogEndpointResponse;
import io.gravitee.reporter.api.v4.log.Log;
import io.reactivex.rxjava3.annotations.Nullable;
import io.reactivex.rxjava3.core.Completable;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class LoggingHook implements InvokerHook {

    @Override
    public String id() {
        return "hook-logging";
    }

    @Override
    public Completable pre(String id, HttpExecutionContext ctx, @Nullable ExecutionPhase executionPhase) {
        return Completable.fromRunnable(() -> {
            final Log log = ctx.metrics().getLog();
            final AnalyticsContext analyticsContext = ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_ANALYTICS_CONTEXT);
            final LoggingContext loggingContext = analyticsContext.getLoggingContext();

            if (log != null && loggingContext != null && loggingContext.endpointRequest()) {
                ((LogEndpointRequest) log.getEndpointRequest()).capture();

                ((HttpExecutionContextInternal) ctx).response().setHeaders(new LogHeadersCaptor(ctx.response().headers()));
            }
        });
    }

    @Override
    public Completable post(String id, HttpExecutionContext ctx, @Nullable ExecutionPhase executionPhase) {
        return Completable.fromRunnable(() -> {
            final Log log = ctx.metrics().getLog();
            final AnalyticsContext analyticsContext = ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_ANALYTICS_CONTEXT);
            final LoggingContext loggingContext = analyticsContext.getLoggingContext();

            if (log != null && loggingContext != null) {
                if (loggingContext.endpointRequest()) {
                    log.getEndpointRequest().setMethod(ctx.request().method());
                }
                if (loggingContext.endpointRequestHeaders()) {
                    log.getEndpointRequest().setHeaders(ctx.request().headers());
                }
            }

            if (log != null && loggingContext != null && loggingContext.endpointResponse()) {
                ((LogEndpointResponse) log.getEndpointResponse()).capture();

                final HttpResponseInternal response = ((HttpExecutionContextInternal) ctx).response();
                response.setHeaders(((LogHeadersCaptor) response.headers()).getDelegate());
            }
        });
    }

    @Override
    public Completable interrupt(String id, HttpExecutionContext ctx, @Nullable ExecutionPhase executionPhase) {
        return post(id, ctx, executionPhase);
    }

    @Override
    public Completable interruptWith(
        String id,
        HttpExecutionContext ctx,
        @Nullable ExecutionPhase executionPhase,
        ExecutionFailure failure
    ) {
        return post(id, ctx, executionPhase);
    }
}
