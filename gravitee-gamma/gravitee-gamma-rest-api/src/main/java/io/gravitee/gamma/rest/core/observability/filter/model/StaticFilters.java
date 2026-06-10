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
 * Host-owned filters with a **fixed definition** that modules cannot extend (unlike
 * {@link ExtensibleFilters}). These are the cross-cutting filters the host always ships
 * (e.g. {@code API}, {@code HTTP_STATUS}).
 *
 * <p>Conceptually host-internal: although the enum is public (the registry, in the infra layer,
 * needs it), modules have no way to contribute to these — the contribution map is keyed by
 * {@link ExtensibleFilters}, and the registry rejects any module filter whose name collides with a
 * host-owned name (see {@link CommonFilters}).
 *
 * @author GraviteeSource Team
 */
public enum StaticFilters {
    API("API", FilterType.KEYWORD, List.of(FilterOperator.EQ, FilterOperator.IN), null, Signal.ALL, ApiType.ALL),
    HTTP_STATUS(
        "HTTP status",
        FilterType.NUMBER,
        List.of(FilterOperator.EQ, FilterOperator.GTE, FilterOperator.LTE),
        new FilterSpec.Range(100, 599),
        Signal.ALL,
        Set.of(ApiType.HTTP_PROXY, ApiType.LLM, ApiType.MCP)
    );

    private final String label;
    private final FilterType type;
    private final List<FilterOperator> operators;
    private final FilterSpec.Range range;
    private final Set<Signal> signals;
    private final Set<ApiType> apiTypes;

    StaticFilters(
        String label,
        FilterType type,
        List<FilterOperator> operators,
        FilterSpec.Range range,
        Set<Signal> signals,
        Set<ApiType> apiTypes
    ) {
        this.label = label;
        this.type = type;
        this.operators = operators;
        this.range = range;
        this.signals = signals;
        this.apiTypes = apiTypes;
    }

    /** Stable filter name echoed on the wire (the enum constant name). */
    public String filterName() {
        return name();
    }

    public FilterSpec toSpec() {
        return new FilterSpec(name(), label, type, operators, null, range, signals, apiTypes);
    }
}
