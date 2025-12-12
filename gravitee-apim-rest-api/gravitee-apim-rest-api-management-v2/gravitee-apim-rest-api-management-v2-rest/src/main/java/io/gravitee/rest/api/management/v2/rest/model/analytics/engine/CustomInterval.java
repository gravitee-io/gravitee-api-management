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
package io.gravitee.rest.api.management.v2.rest.model.analytics.engine;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.Duration;

/**
 * @author GraviteeSource Team
 *
 * Custom interval class to handle both Long (milliseconds) and String (duration format) interval representations.
 */
@JsonDeserialize(using = CustomIntervalDeserializer.class)
@JsonSerialize(using = CustomIntervalSerializer.class)
public class CustomInterval {

    private final Object instanceValue;

    public CustomInterval(String interval) {
        if (interval == null) {
            throw new IllegalArgumentException("Interval cannot be null");
        }
        if (interval.isEmpty()) {
            throw new IllegalArgumentException("Interval cannot be empty");
        }
        instanceValue = interval;
    }

    public CustomInterval(Long interval) {
        if (interval == null) {
            throw new IllegalArgumentException("Interval cannot be null");
        }
        instanceValue = interval;
    }

    public Long toMillis() {
        return switch (instanceValue) {
            case Long value -> value;
            case String value -> parseDurationString(value).toMillis();
            default -> throw new IllegalArgumentException("Interval value is of unknown type");
        };
    }

    private Duration parseDurationString(String duration) {
        if (duration == null || duration.isEmpty()) {
            return Duration.ZERO;
        }

        duration = duration.toUpperCase();
        if (duration.endsWith("D")) {
            return Duration.parse("P" + duration);
        }
        return Duration.parse("PT" + duration);
    }

    public Object getActualInstance() {
        return instanceValue;
    }
}
