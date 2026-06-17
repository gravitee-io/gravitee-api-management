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
package io.gravitee.repository.analytics.engine.api.query;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Canonical HTTP status code group definitions (1XX–5XX). This is the single source of truth
 * for the group→[min,max] inclusive mapping, consumed by:
 * <ul>
 *   <li>ES analytics ({@code FilterAdapter}, {@code HTTPFacetsQueryAdapter})</li>
 *   <li>ES logs ({@code SearchMetricsQueryAdapter} via {@code StatusCodeGroups})</li>
 *   <li>Gamma adapter ({@code ObservabilityLogsDataPortAdapter})</li>
 * </ul>
 */
public final class HttpStatusCodeGroups {

    public record Bounds(int min, int max) {}

    /**
     * Group→bounds mapping in ascending status order. Iteration order is significant:
     * consumers such as {@code HTTPFacetsQueryAdapter} emit ES range aggregations in this
     * order, so a {@link LinkedHashMap} is used rather than an unordered {@link Map#of}.
     */
    public static final Map<String, Bounds> GROUP_BOUNDS;

    /**
     * Human-readable display labels for each status code group, keyed by canonical group name
     * (e.g. {@code "2XX"} → {@code "2xx Success"}). Shared across the Gamma filter catalog
     * ({@code StaticFilters}) and the bucket-name post-processor so that the label is
     * defined in exactly one place.
     */
    public static final Map<String, String> FRIENDLY_LABELS = Map.of(
        "1XX",
        "1xx Informational",
        "2XX",
        "2xx Success",
        "3XX",
        "3xx Redirection",
        "4XX",
        "4xx Client Error",
        "5XX",
        "5xx Server Error"
    );

    static {
        var bounds = new LinkedHashMap<String, Bounds>();
        bounds.put("1XX", new Bounds(100, 199));
        bounds.put("2XX", new Bounds(200, 299));
        bounds.put("3XX", new Bounds(300, 399));
        bounds.put("4XX", new Bounds(400, 499));
        bounds.put("5XX", new Bounds(500, 599));
        GROUP_BOUNDS = Collections.unmodifiableMap(bounds);
    }

    private HttpStatusCodeGroups() {}

    /**
     * Resolves a status code group key (case-insensitive) to its inclusive bounds.
     */
    public static Optional<Bounds> resolve(String group) {
        if (group == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(GROUP_BOUNDS.get(group.toUpperCase(Locale.ROOT)));
    }

    /**
     * Returns all group bounds as {@link NumberRange} for ES range aggregations.
     */
    public static List<NumberRange> asNumberRanges() {
        return GROUP_BOUNDS.values()
            .stream()
            .map(b -> new NumberRange(b.min(), b.max()))
            .toList();
    }

    /**
     * Builds a reverse lookup from ES bucket keys ({@code "100-199"}) to group labels
     * ({@code "1XX"}). ES range aggregation keys use the {@code "min-max"} format.
     */
    public static Map<String, String> esBucketKeyToGroupLabel() {
        return GROUP_BOUNDS.entrySet()
            .stream()
            .collect(Collectors.toUnmodifiableMap(e -> e.getValue().min() + "-" + e.getValue().max(), Map.Entry::getKey));
    }

    /**
     * Builds a reverse lookup from ES bucket keys ({@code "100-199"}) to friendly group labels
     * ({@code "1xx Informational"}). Uses {@link #FRIENDLY_LABELS} as the label source.
     */
    public static Map<String, String> esBucketKeyToFriendlyGroupLabel() {
        return GROUP_BOUNDS.entrySet()
            .stream()
            .collect(
                Collectors.toUnmodifiableMap(
                    e -> e.getValue().min() + "-" + e.getValue().max(),
                    e -> FRIENDLY_LABELS.getOrDefault(e.getKey(), e.getKey())
                )
            );
    }
}
