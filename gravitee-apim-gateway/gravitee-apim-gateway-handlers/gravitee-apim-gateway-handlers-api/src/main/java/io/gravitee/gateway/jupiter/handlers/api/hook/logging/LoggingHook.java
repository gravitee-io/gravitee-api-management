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
package io.gravitee.gateway.jupiter.handlers.api.hook.logging;

import io.gravitee.gateway.core.logging.LoggingContext;
import io.gravitee.gateway.jupiter.api.ExecutionFailure;
import io.gravitee.gateway.jupiter.api.ExecutionPhase;
import io.gravitee.gateway.jupiter.api.context.RequestExecutionContext;
import io.gravitee.gateway.jupiter.api.hook.InvokerHook;
import io.gravitee.gateway.jupiter.core.context.MutableRequestExecutionContext;
import io.gravitee.gateway.jupiter.core.context.MutableResponse;
import io.gravitee.gateway.jupiter.handlers.api.logging.LogHeadersCaptor;
import io.gravitee.gateway.jupiter.handlers.api.logging.request.LogProxyRequest;
import io.gravitee.gateway.jupiter.handlers.api.logging.response.LogProxyResponse;
import io.gravitee.reporter.api.log.Log;
import io.reactivex.Completable;
import io.reactivex.annotations.Nullable;

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
    public Completable pre(String id, RequestExecutionContext ctx, @Nullable ExecutionPhase executionPhase) {
        return Completable.fromRunnable(
            () -> {
                final Log log = ctx.request().metrics().getLog();
                final LoggingContext loggingContext = ctx.getInternalAttribute(LoggingContext.LOGGING_CONTEXT_ATTRIBUTE);

                if (log != null && loggingContext.proxyMode()) {
                    final LogProxyRequest logRequest = new LogProxyRequest(loggingContext, ctx.request());
                    log.setProxyRequest(logRequest);
                    ((MutableRequestExecutionContext) ctx).response().setHeaders(new LogHeadersCaptor(ctx.response().headers()));
                }
            }
        );
    }

    @Override
    public Completable post(String id, RequestExecutionContext ctx, @Nullable ExecutionPhase executionPhase) {
        return Completable.fromRunnable(
            () -> {
                final Log log = ctx.request().metrics().getLog();
                final LoggingContext loggingContext = ctx.getInternalAttribute(LoggingContext.LOGGING_CONTEXT_ATTRIBUTE);

                if (log != null && loggingContext.proxyMode()) {
                    final LogProxyResponse logResponse = new LogProxyResponse(loggingContext, ctx.response());
                    log.setProxyResponse(logResponse);

                    final MutableResponse response = ((MutableRequestExecutionContext) ctx).response();
                    response.setHeaders(((LogHeadersCaptor) response.headers()).getDelegate());
                }
            }
        );
    }

    @Override
    public Completable interrupt(String id, RequestExecutionContext ctx, @Nullable ExecutionPhase executionPhase) {
        return post(id, ctx, executionPhase);
    }

    @Override
    public Completable interruptWith(
        String id,
        RequestExecutionContext ctx,
        @Nullable ExecutionPhase executionPhase,
        ExecutionFailure failure
    ) {
        return post(id, ctx, executionPhase);
    }
}
