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
package io.gravitee.gamma.rest.core.observability.catalog.inmemory;

import io.gravitee.apim.core.analytics_engine.model.AdditionalMetricDiscovery;
import io.gravitee.apim.core.analytics_engine.model.DiscoveredAdditionalMetric;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/** The platform's cap on a discovery pass (OBS-70, §6), so the in-memory port caps as the adapter does. */
final class PlatformDiscoveryCap {

    record Key(String rawKey, Instant lastSeen) {}

    record Capped(List<String> rawKeys, boolean truncated) {}

    private PlatformDiscoveryCap() {}

    static Capped cap(List<Key> keys, boolean truncated, Set<String> uncountedKeys) {
        var capped = new AdditionalMetricDiscovery(
            keys
                .stream()
                .map(key -> new DiscoveredAdditionalMetric(key.rawKey(), key.lastSeen()))
                .toList(),
            Instant.EPOCH,
            truncated
        )
            .capDiscovered(uncountedKeys);
        return new Capped(capped.metrics().stream().map(DiscoveredAdditionalMetric::rawKey).toList(), capped.truncated());
    }
}
