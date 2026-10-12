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
package io.gravitee.gamma.rest.core.observability.analytics.model;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

/**
 * The series a time-series with a {@code limit} may draw, per metric: the top {@code limit} values of the
 * innermost facet over the whole time range, within each combination of the outer facets' values. A
 * series is the list of its facet keys, outermost first.
 *
 * <p>Values are ranked by the first measure the metric plots, descending; ties go to the smaller key, so
 * the selection is stable across refreshes, and a value with no measure comes last.
 *
 * @author GraviteeSource Team
 */
public final class SeriesRanking {

    private static final Comparator<RankedKey> BY_MEASURE_DESC_THEN_KEY = Comparator.comparing(
        RankedKey::value,
        Comparator.nullsLast(Comparator.reverseOrder())
    ).thenComparing(RankedKey::key);

    private final Map<String, Set<List<String>>> seriesByMetric;
    // Every series and every start of one, per metric, so a bucket is checked in constant time.
    private final Map<String, Set<List<String>>> prefixesByMetric;

    public SeriesRanking(Map<String, Set<List<String>>> seriesByMetric) {
        this.seriesByMetric = seriesByMetric
            .entrySet()
            .stream()
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> Set.copyOf(entry.getValue())));
        this.prefixesByMetric = this.seriesByMetric.entrySet()
            .stream()
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> prefixes(entry.getValue())));
    }

    /**
     * @param facetsResponse the engine's facets response over the time range, every value of the facets included
     * @param metrics the time-series metrics, whose first measure ranks their values
     * @param facetCount the number of facets, outermost first
     * @param limit how many innermost values each combination of outer values keeps
     */
    public static SeriesRanking rank(JsonNode facetsResponse, List<AnalyticsFacetMetricQuery> metrics, int facetCount, int limit) {
        var rankingMeasures = new HashMap<String, String>();
        metrics.forEach(metric ->
            rankingMeasures.putIfAbsent(metric.metricName(), metric.measures().isEmpty() ? null : metric.measures().getFirst())
        );

        var seriesByMetric = new HashMap<String, Set<List<String>>>();
        for (var metric : facetsResponse.path("metrics")) {
            var name = metric.path("metric").asText();
            if (!rankingMeasures.containsKey(name)) {
                continue;
            }
            var measure = rankingMeasures.get(name);
            var series = new LinkedHashSet<List<String>>();
            collect(metric.path("buckets"), List.of(), facetCount, measure, limit, series);
            if (!series.isEmpty()) {
                seriesByMetric.merge(name, series, (first, second) -> first);
            }
        }
        return new SeriesRanking(seriesByMetric);
    }

    public boolean isEmpty() {
        return seriesByMetric.isEmpty();
    }

    /** Whether {@code keys}, outermost first, is a ranked series of {@code metric} or the start of one. */
    public boolean includes(String metric, List<String> keys) {
        return prefixesByMetric.getOrDefault(metric, Set.of()).contains(keys);
    }

    /** Every key a ranked series of any metric holds at {@code level}, the outermost facet being level 0. */
    public SortedSet<String> keysAt(int level) {
        var keys = new TreeSet<String>();
        seriesByMetric
            .values()
            .forEach(series ->
                series
                    .stream()
                    .filter(keyPath -> keyPath.size() > level)
                    .forEach(keyPath -> keys.add(keyPath.get(level)))
            );
        return keys;
    }

    /**
     * How many facet levels the ranked series span: fewer than requested when the engine broke the values
     * down by the first facet only, as the single-facet engines do.
     */
    public int depth() {
        return seriesByMetric.values().stream().flatMap(Set::stream).mapToInt(List::size).max().orElse(0);
    }

    private static void collect(
        JsonNode buckets,
        List<String> outerKeys,
        int facetCount,
        String measure,
        int limit,
        Set<List<String>> series
    ) {
        if (outerKeys.size() < facetCount - 1 && isBrokenDown(buckets)) {
            buckets.forEach(bucket ->
                collect(bucket.path("buckets"), append(outerKeys, bucket.path("key").asText()), facetCount, measure, limit, series)
            );
            return;
        }
        StreamSupport.stream(buckets.spliterator(), false)
            .map(bucket -> new RankedKey(bucket.path("key").asText(), measureValue(bucket, measure)))
            .sorted(BY_MEASURE_DESC_THEN_KEY)
            .limit(limit)
            .forEach(ranked -> series.add(append(outerKeys, ranked.key())));
    }

    // A leaf carries no nested bucket list; an outer value with no inner data carries an empty one.
    private static boolean isBrokenDown(JsonNode buckets) {
        return StreamSupport.stream(buckets.spliterator(), false).anyMatch(bucket -> bucket.path("buckets").isArray());
    }

    private static Set<List<String>> prefixes(Set<List<String>> series) {
        var prefixes = new HashSet<List<String>>();
        series.forEach(keys -> IntStream.rangeClosed(1, keys.size()).forEach(size -> prefixes.add(List.copyOf(keys.subList(0, size)))));
        return Set.copyOf(prefixes);
    }

    /** {@code null} when the metric plots no measure: its values are then ranked by key alone. */
    private static Double measureValue(JsonNode bucket, String measure) {
        if (measure == null) {
            return null;
        }
        for (var candidate : bucket.path("measures")) {
            if (measure.equals(candidate.path("name").asText()) && candidate.path("value").isNumber()) {
                return candidate.path("value").asDouble();
            }
        }
        return null;
    }

    private static List<String> append(List<String> keys, String key) {
        var appended = new ArrayList<>(keys);
        appended.add(key);
        return List.copyOf(appended);
    }

    private record RankedKey(String key, Double value) {}
}
