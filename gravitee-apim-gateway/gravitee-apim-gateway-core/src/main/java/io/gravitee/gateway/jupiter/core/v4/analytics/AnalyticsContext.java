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
package io.gravitee.gateway.jupiter.core.v4.analytics;

import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.analytics.sampling.Sampling;
import io.gravitee.gateway.jupiter.core.v4.analytics.sampling.CountMessageSamplingStrategy;
import io.gravitee.gateway.jupiter.core.v4.analytics.sampling.MessageSamplingStrategy;
import io.gravitee.gateway.jupiter.core.v4.analytics.sampling.ProbabilityMessageStrategy;
import io.gravitee.gateway.jupiter.core.v4.analytics.sampling.TemporalMessageSamplingStrategy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@Slf4j
public class AnalyticsContext {

    private Analytics analytics;
    private LoggingContext loggingContext;
    private MessageSamplingStrategy messageSamplingStrategy;

    public AnalyticsContext(
        final Analytics analytics,
        final boolean isEventNative,
        final String loggingMaxsize,
        final String loggingExcludedResponseType
    ) {
        this.analytics = analytics;
        if (analytics != null && analytics.isEnabled()) {
            initMessageSampling(isEventNative);
            initLoggingContext(loggingMaxsize, loggingExcludedResponseType);
        }
    }

    private void initMessageSampling(final boolean isEventNative) {
        if (isEventNative) {
            Sampling messageSampling = this.analytics.getMessageSampling();
            if (messageSampling != null) {
                switch (messageSampling.getType()) {
                    case PROBABILITY:
                        messageSamplingStrategy = new ProbabilityMessageStrategy(messageSampling.getValue());
                        break;
                    case TEMPORAL:
                        messageSamplingStrategy = new TemporalMessageSamplingStrategy(messageSampling.getValue());
                        break;
                    case COUNT:
                        messageSamplingStrategy = new CountMessageSamplingStrategy(messageSampling.getValue());
                        break;
                    default:
                        log.warn("Message sampling type is invalid, using probability strategy by default.");
                        messageSamplingStrategy = new ProbabilityMessageStrategy(null);
                }
            } else {
                log.warn("Message sampling is null, using probability strategy by default.");
                messageSamplingStrategy = new ProbabilityMessageStrategy(null);
            }
        }
    }

    private void initLoggingContext(final String loggingMaxsize, final String loggingExcludedResponseType) {
        if (AnalyticsUtils.isLoggingEnabled(analytics)) {
            this.loggingContext = new LoggingContext(analytics.getLogging());
            this.loggingContext.setMaxSizeLogMessage(loggingMaxsize);
            this.loggingContext.setExcludedResponseTypes(loggingExcludedResponseType);
        }
    }

    public boolean isEnabled() {
        return this.analytics.isEnabled();
    }

    public boolean isLoggingEnabled() {
        return this.loggingContext != null;
    }
}
