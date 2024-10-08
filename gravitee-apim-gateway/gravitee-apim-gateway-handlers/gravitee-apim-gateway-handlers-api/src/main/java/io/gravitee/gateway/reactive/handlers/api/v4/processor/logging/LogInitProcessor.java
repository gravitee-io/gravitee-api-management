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
package io.gravitee.gateway.reactive.handlers.api.v4.processor.logging;

import static io.gravitee.gateway.reactive.api.context.ContextAttributes.ATTR_API;

import io.gravitee.gateway.reactive.api.context.ContextAttributes;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainRequest;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainResponse;
import io.gravitee.gateway.reactive.core.condition.ExpressionLanguageConditionFilter;
import io.gravitee.gateway.reactive.core.context.HttpExecutionContextInternal;
import io.gravitee.gateway.reactive.core.processor.Processor;
import io.gravitee.gateway.reactive.core.v4.analytics.AnalyticsContext;
import io.gravitee.gateway.reactive.core.v4.analytics.LoggingContext;
import io.gravitee.gateway.reactive.handlers.api.v4.analytics.logging.request.LogEndpointRequest;
import io.gravitee.gateway.reactive.handlers.api.v4.analytics.logging.request.LogEntrypointRequest;
import io.gravitee.gateway.reactive.handlers.api.v4.analytics.logging.response.LogEndpointResponse;
import io.gravitee.gateway.reactive.handlers.api.v4.analytics.logging.response.LogEntrypointResponse;
import io.gravitee.reporter.api.v4.log.Log;
import io.reactivex.rxjava3.core.Completable;

/**
 * Processor in charge of initializing the {@link Log} entity during the request phase if logging condition is evaluated to true.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class LogInitProcessor implements Processor {

    public static final String ID = "processor-init-logging";
    private static final ExpressionLanguageConditionFilter<LoggingContext> CONDITION_FILTER = new ExpressionLanguageConditionFilter<>();

    public static LogInitProcessor instance() {
        return Holder.INSTANCE;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Completable execute(final HttpExecutionContextInternal ctx) {
        return Completable.defer(() -> {
            final AnalyticsContext analyticsContext = ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_ANALYTICS_CONTEXT);

            if (!analyticsContext.isLoggingEnabled()) {
                return Completable.complete();
            }

            return CONDITION_FILTER
                .filter(ctx, analyticsContext.getLoggingContext())
                .doOnSuccess(activeLoggingContext -> initLogEntity(ctx, activeLoggingContext))
                .ignoreElement();
        });
    }

    private void initLogEntity(final HttpExecutionContextInternal ctx, final LoggingContext loggingContext) {
        HttpPlainRequest request = ctx.request();
        HttpPlainResponse response = ctx.response();

        Log log = Log
            .builder()
            .timestamp(request.timestamp())
            .requestId(request.id())
            .clientIdentifier(request.clientIdentifier())
            .apiId(ctx.getAttribute(ATTR_API))
            .apiName(ctx.getAttribute(ContextAttributes.ATTR_API_NAME))
            .build();
        ctx.metrics().setLog(log);

        if (loggingContext.entrypointRequest()) {
            final LogEntrypointRequest logRequest = new LogEntrypointRequest(loggingContext, request);
            log.setEntrypointRequest(logRequest);
        }
        if (loggingContext.entrypointResponse()) {
            final LogEntrypointResponse logResponse = new LogEntrypointResponse(loggingContext, response);
            log.setEntrypointResponse(logResponse);
        }
        if (loggingContext.endpointRequest()) {
            final LogEndpointRequest logRequest = new LogEndpointRequest(loggingContext, ctx);
            log.setEndpointRequest(logRequest);
        }
        if (loggingContext.endpointResponse()) {
            final LogEndpointResponse logResponse = new LogEndpointResponse(loggingContext, response);
            log.setEndpointResponse(logResponse);
        }
    }

    private static class Holder {

        private static final LogInitProcessor INSTANCE = new LogInitProcessor();
    }
}
