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
package io.gravitee.gamma.rest.core.observability.filter.model;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Host-owned {@link FilterType#ENUM} filters whose **values are open**: modules extend them by
 * contributing additional {@link FilterSpec.EnumValue}s (see
 * {@code FilterContributor.enumValues()}). This is the **module-facing** part of the catalog — a
 * module references one of these constants to declare which values it adds, and cannot touch
 * anything else (the contribution map is keyed by this enum).
 *
 * <p>Each constant owns its filter shell (label, signals, apiTypes, operators) so the host is the
 * single owner of the definition; modules only add values. The final {@code enumValues} of a filter
 * is {@link #baselineValues()} (host) ∪ module contributions, deduplicated by value.
 *
 * <p><b>Temporary baseline.</b> Until each owning module becomes a real contributor, the host seeds
 * the values itself (e.g. {@code API_TYPE} = the full {@link ApiType} vocabulary). As a module takes
 * ownership of its API kinds, its values must be removed from the host baseline here (they will then
 * come from the module). See GMA-421 note.
 *
 * @author GraviteeSource Team
 */
public enum ExtensibleFilters {
    API_TYPE("API type", Signal.ALL, ApiType.ALL);

    private final String label;
    private final Set<Signal> signals;
    private final Set<ApiType> apiTypes;

    ExtensibleFilters(String label, Set<Signal> signals, Set<ApiType> apiTypes) {
        this.label = label;
        this.signals = signals;
        this.apiTypes = apiTypes;
    }

    /** Stable filter name echoed on the wire (the enum constant name). */
    public String filterName() {
        return name();
    }

    /**
     * Host-provided default values for this filter, merged (union, dedup by value) with module
     * contributions. Temporary for {@code API_TYPE} — see the class note.
     */
    public List<FilterSpec.EnumValue> baselineValues() {
        return switch (this) {
            // TODO(GMA-421): remove API kinds here as their owning module becomes a contributor.
            case API_TYPE -> Arrays.stream(ApiType.values())
                .map(t -> new FilterSpec.EnumValue(t.name(), t.label()))
                .toList();
        };
    }

    /** Builds the {@link FilterSpec} shell for this filter with the given (already merged) values. */
    public FilterSpec toSpec(List<FilterSpec.EnumValue> values) {
        return new FilterSpec(
            name(),
            label,
            FilterType.ENUM,
            List.of(FilterOperator.EQ, FilterOperator.IN),
            values,
            null,
            signals,
            apiTypes
        );
    }
}
