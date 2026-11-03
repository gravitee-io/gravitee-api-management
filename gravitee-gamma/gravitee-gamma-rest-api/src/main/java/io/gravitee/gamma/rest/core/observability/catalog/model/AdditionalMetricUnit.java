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
package io.gravitee.gamma.rest.core.observability.catalog.model;

import io.gravitee.gamma.rest.core.observability.catalog.exception.InvalidMetricDefinitionException;
import java.util.Set;

/**
 * The unit of an additional metric: one of the units the observability library formats, or a free suffix shown as
 * written ({@code €}). {@link #NAMED_UNITS} mirrors the {@code MetricUnit} union of gamma-lib-observability 6.5.0.
 *
 * @author GraviteeSource Team
 */
public final class AdditionalMetricUnit {

    public static final Set<String> NAMED_UNITS = Set.of(
        "PERCENT",
        "MILLISECONDS",
        "SECONDS",
        "BYTES",
        "KILOBYTES",
        "MEGABYTES",
        "GIGABYTES",
        "COUNT",
        "COUNT_PER_SECOND"
    );
    public static final int MAX_SUFFIX_LENGTH = 16;

    private AdditionalMetricUnit() {}

    /** The unit to store: {@code null} for none, a named unit, or a suffix of at most 16 printable characters. */
    public static String validate(String unit) {
        if (unit == null || unit.isBlank()) {
            return null;
        }
        if (NAMED_UNITS.contains(unit)) {
            return unit;
        }
        String suffix = unit.strip();
        if (suffix.length() > MAX_SUFFIX_LENGTH || suffix.chars().anyMatch(Character::isISOControl)) {
            throw InvalidMetricDefinitionException.invalidUnit(unit);
        }
        return suffix;
    }
}
