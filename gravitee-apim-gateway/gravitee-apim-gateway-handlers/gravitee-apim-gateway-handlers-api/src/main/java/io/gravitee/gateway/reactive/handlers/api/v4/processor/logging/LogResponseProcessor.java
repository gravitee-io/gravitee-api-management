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

import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.core.context.HttpExecutionContextInternal;
import io.gravitee.gateway.reactive.core.context.MutableExecutionContext;
import io.gravitee.gateway.reactive.core.processor.Processor;
import io.gravitee.gateway.reactive.core.v4.analytics.AnalyticsContext;
import io.gravitee.gateway.reactive.core.v4.analytics.LoggingContext;
import io.gravitee.gateway.reactive.handlers.api.v4.analytics.logging.response.LogEntrypointResponse;
import io.gravitee.reporter.api.v4.log.Log;
import io.reactivex.rxjava3.core.Completable;

/**
 * Processor allowing to manage response logging.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class LogResponseProcessor implements Processor {

    public static final String ID = "processor-logging-response";

    public static LogResponseProcessor instance() {
        return Holder.INSTANCE;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Completable execute(final HttpExecutionContextInternal ctx) {
        return Completable.fromRunnable(() -> {
            final Log log = ctx.metrics().getLog();
            final AnalyticsContext analyticsContext = ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_ANALYTICS_CONTEXT);
            LoggingContext loggingContext = analyticsContext.getLoggingContext();

            if (log != null && loggingContext.entrypointResponse()) {
                ((LogEntrypointResponse) log.getEntrypointResponse()).capture();
            }
        });
    }

    private static class Holder {

        private static final LogResponseProcessor INSTANCE = new LogResponseProcessor();
    }
}
