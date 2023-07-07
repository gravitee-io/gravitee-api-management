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

import io.gravitee.common.utils.DurationParser;
import io.gravitee.gateway.reactive.api.message.Message;
import java.time.Duration;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter(AccessLevel.PROTECTED)
@Slf4j
public class TemporalMessageSamplingStrategy implements MessageSamplingStrategy {

    private static final Duration DEFAULT_DURATION = Duration.ofSeconds(10);
    private static final Duration DEFAULT_MIN_DURATION = Duration.ofSeconds(1);
    private final long periodInMs;

    public TemporalMessageSamplingStrategy(final String value) {
        Duration duration = DurationParser.parse(value);
        if (duration == null) {
            log.warn("Temporal sampling value '{}' cannot be parsed as Duration, using default value '{}'", value, DEFAULT_DURATION);
            duration = DEFAULT_DURATION;
        } else if (duration.compareTo(DEFAULT_MIN_DURATION) < 0) {
            log.warn(
                "Probability sampling value '{}' is lower than minimum allowed '{}', using min value instead.",
                value,
                DEFAULT_MIN_DURATION
            );
            duration = DEFAULT_MIN_DURATION;
        }
        periodInMs = duration.toMillis();
    }

    @Override
    public boolean isRecordable(final Message message, final int messageCount, final long lastMessageTimestamp) {
        return messageCount == 1 || lastMessageTimestamp == -1 || message.timestamp() - lastMessageTimestamp >= periodInMs;
    }
}
