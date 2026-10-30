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

import io.gravitee.gamma.rest.core.observability.analytics.model.MetricDefinition;
import io.gravitee.gamma.rest.core.observability.analytics.port.service_provider.AnalyticsDefinitionPort;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricDiscovery;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricField;
import io.gravitee.gamma.rest.core.observability.catalog.model.DiscoveredAdditionalMetric;
import io.gravitee.gamma.rest.core.observability.catalog.port.service_provider.AdditionalMetricDiscoveryPort;
import io.gravitee.gamma.rest.core.observability.catalog.port.service_provider.AdditionalMetricGrammarPort;
import io.gravitee.gamma.rest.core.observability.catalog.port.service_provider.BuiltInAdditionalMetricsPort;
import io.gravitee.gamma.rest.core.observability.catalog.port.service_provider.TrafficReadPermissionPort;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.infra.adapter.AdditionalMetricGrammarPortAdapter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Configurable stand-ins for the platform behind the catalog ports: built-in reads, the analytics definition,
 * discovery, traffic right.
 */
public class InMemoryCatalogPorts
    implements BuiltInAdditionalMetricsPort, AnalyticsDefinitionPort, AdditionalMetricDiscoveryPort, TrafficReadPermissionPort {

    public static final Instant REFRESHED_AT = Instant.parse("2026-10-29T09:45:00Z");

    private final Map<String, Set<String>> builtIns = new HashMap<>();
    private final List<MetricDefinition> builtInMetrics = new ArrayList<>();
    private AdditionalMetricDiscovery discovery = AdditionalMetricDiscovery.empty(REFRESHED_AT);
    private boolean readsTraffic = true;
    private Set<String> lastUncountedKeys = Set.of();
    private int discoveries;
    private final AdditionalMetricGrammarPort grammar = new AdditionalMetricGrammarPortAdapter();

    public InMemoryCatalogPorts givenBuiltIn(String rawKey, String... names) {
        builtIns.put(rawKey, Set.of(names));
        return this;
    }

    public InMemoryCatalogPorts givenBuiltInMetric(MetricDefinition metric) {
        builtInMetrics.add(metric);
        return this;
    }

    public InMemoryCatalogPorts givenDiscovered(List<DiscoveredAdditionalMetric> fields, boolean truncated) {
        discovery = new AdditionalMetricDiscovery(fields, REFRESHED_AT, truncated);
        return this;
    }

    public InMemoryCatalogPorts givenReadsTraffic(boolean readsTraffic) {
        this.readsTraffic = readsTraffic;
        return this;
    }

    public void reset() {
        builtIns.clear();
        builtInMetrics.clear();
        discovery = AdditionalMetricDiscovery.empty(REFRESHED_AT);
        readsTraffic = true;
    }

    @Override
    public Map<String, Set<String>> builtInNamesByRawKey() {
        return Map.copyOf(builtIns);
    }

    @Override
    public List<MetricDefinition> metricsOf(ApiType apiKind) {
        return builtInMetrics
            .stream()
            .filter(metric -> metric.apiTypes().contains(apiKind))
            .toList();
    }

    /** The grammar's filter, then the platform's cap, applied to the configured pass, as the adapter applies them. */
    @Override
    public AdditionalMetricDiscovery discover(String organizationId, String environmentId, Set<String> uncountedKeys) {
        discoveries++;
        lastUncountedKeys = Set.copyOf(uncountedKeys);
        var capped = PlatformDiscoveryCap.cap(
            discovery
                .fields()
                .stream()
                .filter(field -> grammar.parse(AdditionalMetricField.NAME_PREFIX + field.rawKey()).isPresent())
                .map(field -> new PlatformDiscoveryCap.Key(field.rawKey(), field.lastSeen()))
                .toList(),
            discovery.truncated(),
            uncountedKeys
        );
        Map<String, DiscoveredAdditionalMetric> byKey = new HashMap<>();
        discovery.fields().forEach(field -> byKey.putIfAbsent(field.rawKey(), field));
        var kept = capped.rawKeys().stream().map(byKey::get).toList();
        return new AdditionalMetricDiscovery(kept, discovery.refreshedAt(), capped.truncated());
    }

    /** How many discoveries were asked. */
    public int discoveries() {
        return discoveries;
    }

    /** The keys the last discovery was told never count against the cap. */
    public Set<String> lastUncountedKeys() {
        return lastUncountedKeys;
    }

    @Override
    public boolean readsTraffic(String organizationId, String environmentId) {
        return readsTraffic;
    }
}
