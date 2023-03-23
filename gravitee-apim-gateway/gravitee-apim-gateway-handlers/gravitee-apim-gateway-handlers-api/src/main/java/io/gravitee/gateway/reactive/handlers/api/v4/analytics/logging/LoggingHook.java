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
package io.gravitee.gateway.reactive.handlers.api.v4.analytics.logging;

import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.api.hook.InvokerHook;
import io.gravitee.gateway.reactive.core.context.MutableExecutionContext;
import io.gravitee.gateway.reactive.core.context.MutableResponse;
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
    public Completable pre(String id, ExecutionContext ctx, @Nullable ExecutionPhase executionPhase) {
        return Completable.fromRunnable(() -> {
            final Log log = ctx.metrics().getLog();
            final AnalyticsContext analyticsContext = ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_ANALYTICS_CONTEXT);
            final LoggingContext loggingContext = analyticsContext.getLoggingContext();

<<<<<<< HEAD:gravitee-apim-gateway/gravitee-apim-gateway-handlers/gravitee-apim-gateway-handlers-api/src/main/java/io/gravitee/gateway/reactive/handlers/api/v4/analytics/logging/LoggingHook.java
            if (log != null && loggingContext != null && loggingContext.endpointRequest()) {
                final LogEndpointRequest logRequest = new LogEndpointRequest(loggingContext, ctx);
                log.setEndpointRequest(logRequest);
=======
            if (log != null && loggingContext.proxyMode()) {
                log.setProxyRequest(new LogProxyRequest(loggingContext, ctx.request()));

>>>>>>> 038918e0c7 (fix: put backend host in proxy request headers log):gravitee-apim-gateway/gravitee-apim-gateway-handlers/gravitee-apim-gateway-handlers-api/src/main/java/io/gravitee/gateway/jupiter/handlers/api/hook/logging/LoggingHook.java
                ((MutableExecutionContext) ctx).response().setHeaders(new LogHeadersCaptor(ctx.response().headers()));
            }
        });
    }

    @Override
    public Completable post(String id, ExecutionContext ctx, @Nullable ExecutionPhase executionPhase) {
        return Completable.fromRunnable(() -> {
            final Log log = ctx.metrics().getLog();
            final AnalyticsContext analyticsContext = ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_ANALYTICS_CONTEXT);
            final LoggingContext loggingContext = analyticsContext.getLoggingContext();

<<<<<<< HEAD:gravitee-apim-gateway/gravitee-apim-gateway-handlers/gravitee-apim-gateway-handlers-api/src/main/java/io/gravitee/gateway/reactive/handlers/api/v4/analytics/logging/LoggingHook.java
            if (log != null && loggingContext != null && loggingContext.endpointResponse()) {
                final LogEndpointResponse logResponse = new LogEndpointResponse(loggingContext, ctx.response());
                log.setEndpointResponse(logResponse);
=======
            if (log != null && loggingContext.proxyMode()) {
                log.getProxyRequest().setHeaders(ctx.request().headers());

                final LogProxyResponse logResponse = new LogProxyResponse(loggingContext, ctx.response());
                log.setProxyResponse(logResponse);
>>>>>>> 038918e0c7 (fix: put backend host in proxy request headers log):gravitee-apim-gateway/gravitee-apim-gateway-handlers/gravitee-apim-gateway-handlers-api/src/main/java/io/gravitee/gateway/jupiter/handlers/api/hook/logging/LoggingHook.java

                final MutableResponse response = ((MutableExecutionContext) ctx).response();
                response.setHeaders(((LogHeadersCaptor) response.headers()).getDelegate());
            }
        });
    }

    @Override
    public Completable interrupt(String id, ExecutionContext ctx, @Nullable ExecutionPhase executionPhase) {
        return post(id, ctx, executionPhase);
    }

    @Override
    public Completable interruptWith(String id, ExecutionContext ctx, @Nullable ExecutionPhase executionPhase, ExecutionFailure failure) {
        return post(id, ctx, executionPhase);
    }
}
