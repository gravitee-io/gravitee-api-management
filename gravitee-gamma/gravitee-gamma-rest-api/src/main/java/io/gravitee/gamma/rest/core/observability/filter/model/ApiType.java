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

import java.util.EnumSet;
import java.util.Set;

/**
 * Observability-curated API kind. Second discriminating axis of a {@link FilterSpec} (alongside
 * {@link Signal}): a filter declares the API kinds it is relevant to, so a consumer can ask for
 * only the filters that make sense for the kind of API it observes (e.g. a native-kafka logs table
 * asks for {@code signal=LOGS&apiType=NATIVE}).
 *
 * <p>This vocabulary mirrors the canonical {@code API_TYPE} filter values already used by the v4
 * analytics/logs engines ({@code HTTP_PROXY, LLM, MESSAGE, MCP, NATIVE, EDGE}) — {@code NATIVE}
 * covers native-kafka APIs. The backend owns the mapping from indexed data to these kinds. It
 * doubles as the authoritative {@code enumValues} of the built-in {@code API_TYPE} filter — single
 * source of truth for both the discriminant and the filter values.
 *
 * @author GraviteeSource Team
 */
public enum ApiType {
    HTTP_PROXY("HTTP Proxy"),
    LLM("LLM"),
    MESSAGE("Message"),
    MCP("MCP"),
    A2A("A2A"),
    NATIVE("Kafka (native)"),
    EDGE("Edge"),
    AUTHZ("Authz decision"),
    // Not an API kind: the observability library matches a filter's apiTypes against the value of the
    // field named by its scopeFilterField, which on the decisions screen is RECORD_TYPE. A filter is
    // only offered there if this token is among its apiTypes, so carrying it is how a filter opts in
    // to that screen. Declare {@link #API_KINDS} instead of {@link #ALL} to stay out of it.
    AUTHZ_DECISION("Authz decision record");

    private final String label;

    ApiType(String label) {
        this.label = label;
    }

    /** Human-readable display label for this API kind (e.g. {@code NATIVE} → "Kafka (native)"). */
    public String label() {
        return label;
    }

    /**
     * Every token, API kinds and the decision scope alike. Use for filters that apply everywhere a
     * row can be listed, including the decisions screen (e.g. {@code API}), so they automatically
     * cover kinds added later, while a filter declaring an explicit subset (e.g. {@code {LLM, MCP}})
     * never widens on its own.
     */
    public static final Set<ApiType> ALL = Set.of(values());

    /**
     * Every real API kind, i.e. {@link #ALL} minus {@link #AUTHZ_DECISION}. Use for anything that
     * only makes sense against an API: the values a user may pick in the {@code API_TYPE} filter, and
     * filters carried solely by request documents.
     */
    public static final Set<ApiType> API_KINDS = Set.copyOf(EnumSet.complementOf(EnumSet.of(AUTHZ_DECISION)));
}
