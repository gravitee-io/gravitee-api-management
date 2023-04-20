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
package io.gravitee.apim.plugin.reactor.core.analytics;

import io.gravitee.apim.plugin.reactor.core.analytics.sampling.CountMessageSamplingStrategy;
import io.gravitee.apim.plugin.reactor.core.analytics.sampling.MessageSamplingStrategy;
import io.gravitee.apim.plugin.reactor.core.analytics.sampling.ProbabilityMessageStrategy;
import io.gravitee.apim.plugin.reactor.core.analytics.sampling.TemporalMessageSamplingStrategy;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.analytics.sampling.Sampling;
import io.gravitee.gateway.reactive.core.analytics.AnalyticsContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@Slf4j
public class V4AnalyticsContext extends AnalyticsContext {

    private MessageSamplingStrategy messageSamplingStrategy;

    public V4AnalyticsContext(Analytics analytics, boolean isEventNative, String loggingMaxsize, String loggingExcludedResponseType) {
        super(analytics, loggingMaxsize, loggingExcludedResponseType);
        if (analytics != null && isEnabled() && isEventNative) {
            initMessageSampling();
        }
    }

    private void initMessageSampling() {
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
