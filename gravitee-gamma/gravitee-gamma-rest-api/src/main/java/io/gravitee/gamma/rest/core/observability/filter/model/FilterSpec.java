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

import java.util.List;
import java.util.Set;

/**
 * Self-describing specification of one filterable field in the observability domain. The UI
 * introspects the list returned by the filter-definitions endpoint and renders a generic chip /
 * multi-select / range input depending on {@link #type} and {@link #operators}.
 *
 * <p>{@code name} is the stable id sent back as {@code FilterCondition.name} when the UI submits a
 * filtered query. Contributors should namespace their filters to avoid collisions — the registry
 * uses a simple last-wins union, no explicit override mechanism.
 *
 * @param name        Stable identifier echoed by the UI on filter submission.
 * @param label       Human-readable display label.
 * @param type        Data shape — see {@link FilterType}.
 * @param operators   Supported comparison operators — see {@link FilterOperator}. Always non-empty.
 * @param enumValues  Allowed {@link EnumValue}s (stable value + display label) when {@link #type}
 *                    is {@link FilterType#ENUM} — otherwise null.
 * @param range       Inclusive min / max bounds when {@link #type} is {@link FilterType#NUMBER} and
 *                    a finite range applies — otherwise null.
 * @param signals     Observability signals this filter applies to. A filter with
 *                    {@code signals = {LOGS, TRACES}} appears in both log and trace queries.
 * @param apiTypes    API kinds this filter is relevant to. A filter with {@code apiTypes = {LLM}}
 *                    only surfaces for LLM APIs; use {@link ApiType#ALL} for cross-cutting filters.
 *                    The two axes are independent: applicability is the rectangle
 *                    {@code signals × apiTypes}.
 *
 * @author GraviteeSource Team
 */
public record FilterSpec(
    String name,
    String label,
    FilterType type,
    List<FilterOperator> operators,
    List<EnumValue> enumValues,
    Range range,
    Set<Signal> signals,
    Set<ApiType> apiTypes
) {
    /** Inclusive numeric bounds for {@link FilterType#NUMBER} filters. */
    public record Range(Number min, Number max) {}

    /**
     * One allowed value of an {@link FilterType#ENUM} filter: a stable {@code value} sent on the
     * wire and a human-readable {@code label} for display (e.g. {@code value="NATIVE"},
     * {@code label="Kafka (native)"}).
     */
    public record EnumValue(String value, String label) {}
}
