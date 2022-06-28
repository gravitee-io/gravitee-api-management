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

import io.gravitee.gateway.core.logging.LoggingContext;
import io.gravitee.gateway.jupiter.api.context.RequestExecutionContext;
import io.gravitee.gateway.jupiter.core.processor.Processor;
import io.gravitee.gateway.jupiter.handlers.api.logging.response.LogClientResponse;
import io.gravitee.reporter.api.log.Log;
import io.reactivex.Completable;

/**
 * Processor allowing to manage response logging.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class LogResponseProcessor implements Processor {

    public static final String ID = "processor-logging-response";

    public static LogResponseProcessor instance() {
        return LogResponseProcessor.Holder.INSTANCE;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Completable execute(RequestExecutionContext ctx) {
        return Completable.fromRunnable(
            () -> {
                final Log log = ctx.request().metrics().getLog();
                final LoggingContext loggingContext = ctx.getInternalAttribute(LoggingContext.LOGGING_CONTEXT_ATTRIBUTE);

                if (log != null && loggingContext.clientMode()) {
                    final LogClientResponse logResponse = new LogClientResponse(loggingContext, ctx.response());
                    log.setClientResponse(logResponse);
                }
            }
        );
    }

    private static class Holder {

        private static final LogResponseProcessor INSTANCE = new LogResponseProcessor();
    }
}
