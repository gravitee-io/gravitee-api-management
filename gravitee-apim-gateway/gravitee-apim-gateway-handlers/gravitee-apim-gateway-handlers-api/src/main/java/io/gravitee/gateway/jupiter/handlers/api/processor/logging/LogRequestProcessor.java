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
package io.gravitee.gateway.jupiter.handlers.api.processor.logging;

import static io.gravitee.gateway.core.logging.LoggingContext.LOGGING_CONTEXT_ATTRIBUTE;
import static io.gravitee.gateway.jupiter.api.context.ExecutionContext.ATTR_API;

import io.gravitee.gateway.core.logging.LoggingContext;
import io.gravitee.gateway.jupiter.api.context.Request;
import io.gravitee.gateway.jupiter.api.context.RequestExecutionContext;
import io.gravitee.gateway.jupiter.core.condition.ExpressionLanguageConditionFilter;
import io.gravitee.gateway.jupiter.core.processor.Processor;
import io.gravitee.gateway.jupiter.handlers.api.logging.request.LogClientRequest;
import io.gravitee.reporter.api.log.Log;
import io.reactivex.Completable;

/**
 * Processor in charge of initializing the {@link Log} entity during the request phase if logging condition is evaluated to true.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class LogRequestProcessor implements Processor {

    public static final String ID = "processor-logging-request";
    private static final ExpressionLanguageConditionFilter<LoggingContext> CONDITION_FILTER = new ExpressionLanguageConditionFilter<>();

    public static LogRequestProcessor instance() {
        return LogRequestProcessor.Holder.INSTANCE;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Completable execute(final RequestExecutionContext ctx) {
        return Completable.defer(
            () -> {
                final LoggingContext loggingContext = ctx.getInternalAttribute(LOGGING_CONTEXT_ATTRIBUTE);

                if (loggingContext == null) {
                    return Completable.complete();
                }

                return CONDITION_FILTER
                    .filter(ctx, loggingContext)
                    .doOnSuccess(activeLoggingContext -> initLogEntity(ctx, activeLoggingContext))
                    .ignoreElement();
            }
        );
    }

    private void initLogEntity(final RequestExecutionContext ctx, final LoggingContext loggingContext) {
        final Request request = ctx.request();
        final Log log = new Log(request.timestamp());

        request.metrics().setLog(log);
        log.setRequestId(request.id());
        log.setApi(ctx.getAttribute(ATTR_API));

        if (loggingContext.clientMode()) {
            final LogClientRequest logRequest = new LogClientRequest(loggingContext, request);
            log.setClientRequest(logRequest);
        }
    }

    private static class Holder {

        private static final LogRequestProcessor INSTANCE = new LogRequestProcessor();
    }
}
