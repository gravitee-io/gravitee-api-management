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
package io.gravitee.apim.core.analytics_engine.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The additional metrics found on the requests of an environment by one discovery pass.
 *
 * @param metrics     most recently seen first, then by key
 * @param refreshedAt when the pass ran
 * @param truncated   true when some keys may be missing: the pass did not probe every key of the mapping, ran out of
 *                    time or failed, or {@link #capDiscovered} left some out
 */
public record AdditionalMetricDiscovery(List<DiscoveredAdditionalMetric> metrics, Instant refreshedAt, boolean truncated) {
    /** Discovered entries an environment lists at most. */
    public static final int MAX_DISCOVERED_ENTRIES = 200;

    /** What a pass that failed answers: nothing, flagged as incomplete. */
    public static AdditionalMetricDiscovery unavailable(Instant refreshedAt) {
        return new AdditionalMetricDiscovery(List.of(), refreshedAt, true);
    }

    /** The keys {@code kept} accepts, before any cap: a key a caller cannot list must take no one's place. */
    public AdditionalMetricDiscovery retainKeys(Predicate<String> kept) {
        return new AdditionalMetricDiscovery(
            metrics
                .stream()
                .filter(metric -> kept.test(metric.rawKey()))
                .toList(),
            refreshedAt,
            truncated
        );
    }

    /**
     * Keeps every key of {@code uncountedKeys} and the {@link #MAX_DISCOVERED_ENTRIES} most recently seen of the
     * others, most recently seen first, then by key. Applied once the keys of the environment are known, never on the
     * keys of the whole cluster, so an environment whose keys sort late is not starved. Not called yet: the Gamma filter
     * catalog of OBS-74 applies it once it knows the built-in and declared keys.
     *
     * @param uncountedKeys keys listed under another origin, which never count against the cap and are never left out:
     *                      the keys built-ins read and the declared ones
     */
    public AdditionalMetricDiscovery capDiscovered(Set<String> uncountedKeys) {
        var kept = new ArrayList<DiscoveredAdditionalMetric>();
        var discovered = 0;
        var leftOut = false;
        for (var metric : mostRecentFirst(metrics)) {
            if (uncountedKeys.contains(metric.rawKey())) {
                kept.add(metric);
            } else if (discovered < MAX_DISCOVERED_ENTRIES) {
                kept.add(metric);
                discovered++;
            } else {
                leftOut = true;
            }
        }
        return new AdditionalMetricDiscovery(List.copyOf(kept), refreshedAt, truncated || leftOut);
    }

    private static List<DiscoveredAdditionalMetric> mostRecentFirst(List<DiscoveredAdditionalMetric> metrics) {
        return metrics
            .stream()
            .sorted(Comparator.comparing(DiscoveredAdditionalMetric::lastSeen).reversed().thenComparing(DiscoveredAdditionalMetric::rawKey))
            .toList();
    }
}
