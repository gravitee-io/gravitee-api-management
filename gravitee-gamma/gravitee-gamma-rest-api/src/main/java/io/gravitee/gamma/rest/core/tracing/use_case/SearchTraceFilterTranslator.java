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
package io.gravitee.gamma.rest.core.tracing.use_case;

import io.gravitee.gamma.rest.core.tracing.exception.UnsupportedFilterException;
import io.gravitee.gamma.rest.core.tracing.model.FilterCondition;
import io.gravitee.gamma.rest.core.tracing.model.FilterOperator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Slim-cut translation of UI {@link FilterCondition}s into the attribute-filter map shape the
 * existing {@code TracingPort.searchTraces} signature already accepts. Each supported filter name
 * gets a single hardcoded mapping to the underlying OTel span-attribute key — the abstraction the
 * UI sees is intentional ({@code STATUS} is more discoverable than {@code attributes['otel.status_code']}).
 *
 * <p>Operator scope today: <b>{@code eq} only</b>. Other operators (and the 4 filters that need
 * top-level or range / aggregation rendering — {@code HAS_ERROR}, {@code DURATION_NANOS},
 * {@code OPERATION_NAME}, {@code SPAN_KIND}) are deferred to the follow-up PR that extends the
 * {@code TraceFilterContributor} SPI with per-filter translation logic. The discovery endpoint's
 * {@link io.gravitee.gamma.rest.infra.contributor.CommonTraceFilterContributor} is trimmed to
 * match what this translator supports — drift between the two surfaces is caught by
 * {@code TraceFilterContributorContractTest}.
 *
 * <p>Unknown filter names or unsupported operators throw {@link UnsupportedFilterException},
 * mapped to HTTP 400 — the UI gets a machine-readable {@code technicalCode} to distinguish the
 * cases.
 *
 * @author GraviteeSource Team
 */
public final class SearchTraceFilterTranslator {

    /**
     * Filter-name → underlying OTel attribute key. Single source of truth for the slim cut; the
     * full PR will replace this with per-contributor translation logic so a module's filters live
     * end-to-end inside its own SPI implementation.
     *
     * <p>Every entry MUST be advertised by a contributor: a filter offered by discovery but missing
     * here answers 400 the moment a user picks it. {@code TraceFilterContributorContractTest} checks
     * that direction on every registered contributor. Cross-module (HTTP) entries pair with
     * {@code CommonTraceFilterContributor}; the {@code KAFKA_*} / {@code ERROR_ORIGIN} entries pair
     * with {@code EsmTraceFilterContributor} and use the attribute keys the native Kafka reactor
     * emits ({@code KafkaTracingHelper}).
     */
    private static final Map<String, String> SUPPORTED_ATTRIBUTE_BY_FILTER_NAME = Map.ofEntries(
        Map.entry("HTTP_METHOD", "http.method"),
        Map.entry("HTTP_STATUS_CODE", "http.status_code"),
        Map.entry("HTTP_ROUTE", "http.route"),
        // ESM — native Kafka. `kafka.api.key` carries the protocol request name (PRODUCE, FETCH…),
        // set on every per-request span by KafkaTracing.
        Map.entry("KAFKA_API_KEY", "kafka.api.key"),
        Map.entry("KAFKA_CLIENT_ID", "messaging.client.id"),
        Map.entry("KAFKA_CONSUMER_GROUP", "messaging.consumer.group.name"),
        // Root-span attribute fed by KafkaTracingError: policy / security / config / broker / internal.
        Map.entry("ERROR_ORIGIN", "gravitee.error.origin")
    );

    private SearchTraceFilterTranslator() {}

    /**
     * Walks the caller-supplied conditions and returns the equivalent attribute-filter map.
     * Order-insensitive — the SPI's {@code TraceSearchCriteria} accepts a {@code Map<String,String>}
     * which dedupes by key anyway. Duplicate filter names with conflicting values: last-wins,
     * matching the existing map semantics; the UI shouldn't emit duplicates today.
     */
    public static Map<String, String> toAttributeFilters(List<FilterCondition> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return Map.of();
        }
        Map<String, String> result = new HashMap<>();
        for (FilterCondition condition : conditions) {
            String attributeKey = SUPPORTED_ATTRIBUTE_BY_FILTER_NAME.get(condition.name());
            if (attributeKey == null) {
                throw UnsupportedFilterException.unknownName(condition.name());
            }
            if (condition.operator() != FilterOperator.EQ) {
                throw UnsupportedFilterException.unsupportedOperator(condition.name(), condition.operator().name().toLowerCase());
            }
            // EQ + values.size() != 1 is a UI mistake — eq is intrinsically single-valued; the
            // wire shape uses a list for forward-compat with `in` (next PR), not for multi-eq.
            if (condition.values() == null || condition.values().size() != 1) {
                throw UnsupportedFilterException.unsupportedOperator(
                    condition.name(),
                    "eq with " + (condition.values() == null ? 0 : condition.values().size()) + " values"
                );
            }
            result.put(attributeKey, condition.values().get(0));
        }
        return result;
    }
}
