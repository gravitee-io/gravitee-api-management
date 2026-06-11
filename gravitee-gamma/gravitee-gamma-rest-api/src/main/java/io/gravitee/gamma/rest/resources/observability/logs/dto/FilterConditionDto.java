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
package io.gravitee.gamma.rest.resources.observability.logs.dto;

import io.gravitee.gamma.rest.core.observability.filter.exception.UnsupportedObservabilityFilterException;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterCondition;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterOperator;
import java.util.List;

/**
 * Wire shape for one filter condition on the POST search body. Same polymorphic {@code value}
 * contract as the tracing endpoint's {@code FilterConditionDto}: scalar for
 * {@code EQ}/{@code GTE}/{@code LTE}, array for {@code IN}. Operator is UPPERCASE on the wire.
 */
public record FilterConditionDto(String name, String operator, Object value) {
    public FilterCondition toCore() {
        if (name == null || name.isBlank()) {
            throw UnsupportedObservabilityFilterException.unknownName(name);
        }
        if (operator == null || operator.isBlank()) {
            throw UnsupportedObservabilityFilterException.unsupportedOperator(name, "(missing)");
        }
        FilterOperator op;
        try {
            op = FilterOperator.valueOf(operator.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw UnsupportedObservabilityFilterException.unsupportedOperator(name, operator);
        }
        return new FilterCondition(name, op, normalizeValues(op, value));
    }

    private static List<String> normalizeValues(FilterOperator op, Object value) {
        if (value == null) {
            return List.of();
        }
        if (op == FilterOperator.IN || op == FilterOperator.NOT_IN) {
            if (value instanceof List<?> list) {
                return list.stream().map(FilterConditionDto::asString).toList();
            }
            return List.of(asString(value));
        }
        if (value instanceof List<?> list) {
            return list.stream().map(FilterConditionDto::asString).toList();
        }
        return List.of(asString(value));
    }

    private static String asString(Object v) {
        return v == null ? "" : v.toString();
    }
}
