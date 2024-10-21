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
package io.gravitee.gateway.reactive.core.v4.analytics;

import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.analytics.logging.Logging;
import io.gravitee.definition.model.v4.analytics.tracing.Tracing;
import io.gravitee.gateway.opentelemetry.TracingContext;
import io.gravitee.node.api.configuration.Configuration;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@Slf4j
public class AnalyticsContext {

    protected Analytics analytics;
    protected LoggingContext loggingContext;
    protected TracingContext tracingContext;

    public AnalyticsContext(
        final Analytics analytics,
        final String loggingMaxsize,
        final String loggingExcludedResponseType,
        final TracingContext tracingContext
    ) {
        this.analytics = analytics;
        if (isEnabled()) {
            initLoggingContext(loggingMaxsize, loggingExcludedResponseType);
            initTracingContext(tracingContext);
        }
    }

    private void initLoggingContext(final String loggingMaxsize, final String loggingExcludedResponseType) {
        if (AnalyticsUtils.isLoggingEnabled(analytics)) {
            this.loggingContext = loggingContext(analytics.getLogging());
            this.loggingContext.setMaxSizeLogMessage(loggingMaxsize);
            this.loggingContext.setExcludedResponseTypes(loggingExcludedResponseType);
        }
    }

    private void initTracingContext(final TracingContext tracingContext) {
        if (tracingContext.isEnabled()) {
            this.tracingContext = tracingContext;
        }
    }

    protected LoggingContext loggingContext(Logging logging) {
        return new LoggingContext(logging);
    }

    public boolean isEnabled() {
        return this.analytics != null && this.analytics.isEnabled();
    }

    public boolean isLoggingEnabled() {
        return this.loggingContext != null;
    }

    public boolean isTracingEnabled() {
        return this.tracingContext != null && this.tracingContext.isEnabled();
    }
}
