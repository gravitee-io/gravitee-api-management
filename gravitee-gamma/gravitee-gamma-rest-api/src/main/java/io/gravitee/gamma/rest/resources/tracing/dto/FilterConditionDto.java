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
package io.gravitee.gamma.rest.resources.tracing.dto;

import io.gravitee.gamma.rest.core.tracing.exception.UnsupportedFilterException;
import io.gravitee.gamma.rest.core.tracing.model.FilterCondition;
import io.gravitee.gamma.rest.core.tracing.model.FilterOperator;
import java.util.List;

/**
 * Wire shape for one filter condition on the POST search body. Mirrors the apim management v2
 * {@code Filter} schema discriminated by {@code operator}: {@code value} is polymorphic — a single
 * string for {@code EQ} / {@code NEQ} / {@code CONTAINS}, an array for {@code IN} / {@code NOT_IN},
 * a number for {@code GTE} / {@code LTE}.
 *
 * <p>{@code operator} is UPPERCASE on the wire (matches apim's analytics + logs convention). The
 * lib's TS {@code FilterOperator} union is lowercase; the lib's {@code toWireFilter} uppercases
 * before sending.
 */
public record FilterConditionDto(String name, String operator, Object value) {
    public FilterCondition toCore() {
        if (name == null || name.isBlank()) {
            throw UnsupportedFilterException.unknownName(name);
        }
        if (operator == null || operator.isBlank()) {
            throw UnsupportedFilterException.unsupportedOperator(name, "(missing)");
        }
        FilterOperator op;
        try {
            op = FilterOperator.valueOf(operator.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw UnsupportedFilterException.unsupportedOperator(name, operator);
        }
        return new FilterCondition(name, op, normalizeValues(op, value));
    }

    /**
     * Normalises the polymorphic wire {@code value} to the core's {@code List<String>}. Slim cut
     * only ships {@code EQ}, so most of the branches are forward-looking; they're here so the wire
     * contract documents the full shape even before the translator handles every operator.
     */
    private static List<String> normalizeValues(FilterOperator op, Object value) {
        if (value == null) {
            return List.of();
        }
        // IN / NOT_IN expect an array on the wire; the rest expect a scalar (string or number).
        if (op == FilterOperator.IN || op == FilterOperator.NOT_IN) {
            if (value instanceof List<?> list) {
                return list.stream().map(FilterConditionDto::asString).toList();
            }
            // Single value sent under an array-shaped operator — tolerate by wrapping.
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
