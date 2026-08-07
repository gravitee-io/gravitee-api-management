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
package io.gravitee.gamma.rest.resources.observability.dashboards.dto;

import io.gravitee.gamma.rest.core.observability.dashboard.exception.InvalidDashboardException;
import io.gravitee.gamma.rest.core.observability.dashboard.model.DashboardFilter;
import io.gravitee.gamma.rest.core.observability.filter.exception.UnsupportedObservabilityFilterException;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterCondition;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterOperator;
import io.gravitee.gamma.rest.resources.observability.dto.FilterValues;

/**
 * Request shape for one dashboard filter (OBS-16). {@code value} is polymorphic — scalar or array,
 * normalized by {@link FilterValues}; an empty array is the legal "all values" placeholder.
 * {@code editable} is a boxed {@link Boolean} defaulting to {@code false} (locked) when absent — the
 * restrictive default, which can never widen a scope by accident — then narrowed to the primitive
 * the core model carries. The name is deliberately not checked against the filter catalogue: a
 * dashboard is declarative configuration, validated at query time, and catalogue coupling would make
 * dashboards non-importable across environments.
 */
public record SaveDashboardFilterDto(String name, String label, String operator, Object value, Boolean editable) {
    public DashboardFilter toCore() {
        if (name == null || name.isBlank()) {
            throw new InvalidDashboardException("Dashboard filter name is required");
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
        return new DashboardFilter(new FilterCondition(name, op, FilterValues.normalize(value)), label, editable != null && editable);
    }
}
