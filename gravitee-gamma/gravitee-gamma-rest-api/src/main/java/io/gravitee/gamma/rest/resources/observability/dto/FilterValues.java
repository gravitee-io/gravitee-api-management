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
package io.gravitee.gamma.rest.resources.observability.dto;

import io.gravitee.gamma.rest.core.observability.filter.exception.UnsupportedObservabilityFilterException;
import java.util.List;

/**
 * Shared normalization for the polymorphic filter {@code value} the observability wire contracts
 * accept: a scalar for single-value operators ({@code EQ}, {@code GTE}, {@code LTE}), an array for
 * {@code IN}/{@code NOT_IN} — either shape collapses to the {@code List<String>} the core
 * {@code FilterCondition} carries. Promoted out of the logs {@code FilterConditionDto} (OBS-16) so
 * the dashboards write DTOs reuse it instead of copying it. {@code null} and {@code []} both
 * normalize to an empty list — the "all values" placeholder, which is legal and carries no query
 * constraint. Anything else — an object, a nested array, a null element — is refused rather than
 * stringified into a value no document holds.
 *
 * @author GraviteeSource Team
 */
public final class FilterValues {

    private FilterValues() {}

    public static List<String> normalize(String filterName, Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            return list
                .stream()
                .map(element -> asScalar(filterName, element))
                .toList();
        }
        return List.of(asScalar(filterName, value));
    }

    private static String asScalar(String filterName, Object value) {
        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        throw UnsupportedObservabilityFilterException.invalidValueShape(filterName);
    }
}
