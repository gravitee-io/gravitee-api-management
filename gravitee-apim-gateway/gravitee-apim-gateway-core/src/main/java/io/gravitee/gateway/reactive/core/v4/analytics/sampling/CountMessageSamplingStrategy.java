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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@Getter(AccessLevel.PROTECTED)
public class CountMessageSamplingStrategy implements MessageSamplingStrategy {

    private static final int DEFAULT_COUNT = 100;
    private static final int DEFAULT_MIN_COUNT = 10;
    private int count;

    public CountMessageSamplingStrategy(final String value) {
        try {
            this.count = Integer.parseInt(value);
            if (this.count < DEFAULT_MIN_COUNT) {
                log.warn(
                    "Probability sampling value '{}' is lower than minimum allowed '{}', using min value instead.",
                    value,
                    DEFAULT_MIN_COUNT
                );
                this.count = DEFAULT_MIN_COUNT;
            }
        } catch (Exception e) {
            log.warn("Count sampling value '{}' cannot be parsed as Integer, using default value '{}'", value, DEFAULT_COUNT);
            this.count = DEFAULT_COUNT;
        }
    }

    @Override
    public boolean isRecordable(final Message message, final int messageCount, final long lastMessageTimestamp) {
        return messageCount == 1 || messageCount % count == 1;
    }
}
