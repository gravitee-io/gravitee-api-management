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
package io.gravitee.definition.model.v4.analytics.sampling;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import io.gravitee.common.utils.DurationParser;
import java.time.Duration;
import java.util.Map;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public enum SamplingType {
    PROBABILITY(
        "probability",
        value -> {
            try {
                double parseDouble = Double.parseDouble(value);
                return parseDouble >= 0 && parseDouble <= 1;
            } catch (Exception e) {
                return false;
            }
        }
    ),
    TEMPORAL(
        "temporal",
        value -> {
            try {
                Duration duration = DurationParser.parse(value);
                return duration != null;
            } catch (Exception e) {
                return false;
            }
        }
    ),
    COUNT(
        "count",
        value -> {
            try {
                int count = Integer.parseInt(value);
                return count > 0;
            } catch (Exception e) {
                return false;
            }
        }
    );

    private static final Map<String, SamplingType> LABELS_MAP = Map.of(
        PROBABILITY.label,
        PROBABILITY,
        TEMPORAL.label,
        TEMPORAL,
        COUNT.label,
        COUNT
    );

    @JsonValue
    private final String label;

    @JsonIgnore
    private final Function<String, Boolean> validationFunction;

    public String getLabel() {
        return label;
    }

    public boolean validate(final String value) {
        return validationFunction.apply(value);
    }

    public static SamplingType fromLabel(final String label) {
        if (label != null) {
            return LABELS_MAP.get(label);
        }
        return null;
    }
}
