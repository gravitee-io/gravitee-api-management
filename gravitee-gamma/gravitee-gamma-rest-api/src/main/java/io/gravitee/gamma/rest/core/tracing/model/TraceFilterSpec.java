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
package io.gravitee.gamma.rest.core.tracing.model;

import java.util.List;

/**
 * Self-describing specification of one filterable field on the trace explorer. The UI introspects
 * the list returned by the filter-definitions endpoint and renders a generic chip / multi-select /
 * range input depending on {@link #type} and {@link #operators}.
 *
 * <p>{@code name} is the stable id sent back as {@code FilterCondition.field} when the UI submits a
 * filtered search — modules that ship contributors should namespace their own filters to avoid
 * collisions with the built-in cross-module ones (the registry uses a simple last-wins union, no
 * explicit override mechanism).
 *
 * @param name        Stable identifier echoed by the UI on filter submission.
 * @param label       Human-readable display label.
 * @param type        Data shape — see {@link FilterType}.
 * @param operators   Supported comparison operators — see {@link FilterOperator}. Always non-empty.
 * @param enumValues  Allowed values when {@link #type} is {@link FilterType#ENUM} — otherwise null.
 * @param range       Inclusive min / max bounds when {@link #type} is {@link FilterType#NUMBER} and
 *                    a finite range applies — otherwise null.
 *
 * @author GraviteeSource Team
 */
public record TraceFilterSpec(
    String name,
    String label,
    FilterType type,
    List<FilterOperator> operators,
    List<String> enumValues,
    Range range
) {
    /** Inclusive numeric bounds for {@link FilterType#NUMBER} filters. */
    public record Range(Number min, Number max) {}
}
