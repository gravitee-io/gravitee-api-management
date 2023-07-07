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
package io.gravitee.gateway.reactive.core.v4.analytics.sampling;

import io.gravitee.gateway.reactive.api.message.Message;
import java.util.SplittableRandom;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@Getter(AccessLevel.PROTECTED)
public class ProbabilityMessageStrategy implements MessageSamplingStrategy {

    private static final Double DEFAULT_PROBABILITY = 0.01;
    private static final Double DEFAULT_MAX_PROBABILITY = 0.5;
    private final SplittableRandom random = new SplittableRandom();
    private double probability;

    public ProbabilityMessageStrategy(final String value) {
        try {
            this.probability = Double.parseDouble(value);
            if (this.probability > DEFAULT_MAX_PROBABILITY) {
                log.warn(
                    "Probability sampling value '{}' is higher than maximum allowed '{}', using max value instead.",
                    value,
                    DEFAULT_MAX_PROBABILITY
                );
                this.probability = DEFAULT_MAX_PROBABILITY;
            }
        } catch (Exception e) {
            log.warn("Probability sampling value '{}' cannot be parsed as Double, using default value '{}'", value, DEFAULT_PROBABILITY);
            this.probability = DEFAULT_PROBABILITY;
        }
    }

    @Override
    public boolean isRecordable(final Message message, final int messageCount, final long lastMessageTimestamp) {
        return messageCount == 1 || matchesProbability(random.nextDouble());
    }

    protected boolean matchesProbability(final Double random) {
        return random < probability;
    }
}
