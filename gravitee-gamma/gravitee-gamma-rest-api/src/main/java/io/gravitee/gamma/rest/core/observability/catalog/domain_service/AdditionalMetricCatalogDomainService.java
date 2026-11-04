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
package io.gravitee.gamma.rest.core.observability.catalog.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.gamma.rest.core.observability.analytics.model.MetricDefinition;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricCatalog;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricCatalogEntry;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricDefinition;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricDiscovery;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricField;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricValueType;
import io.gravitee.gamma.rest.core.observability.catalog.model.CatalogFlags;
import io.gravitee.gamma.rest.core.observability.catalog.model.CatalogOrigin;
import io.gravitee.gamma.rest.core.observability.catalog.model.DiscoveredAdditionalMetric;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterOperator;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterSpec;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterType;
import io.gravitee.gamma.rest.core.observability.filter.model.Signal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;

/**
 * Merges the three origins of the environment's additional metrics into one entry per key, resolved
 * {@code BUILT_IN} › {@code DECLARED} › {@code DISCOVERED}, with the label precedence declared › built-in › derived.
 *
 * @author GraviteeSource Team
 */
@DomainService
@AllArgsConstructor
public class AdditionalMetricCatalogDomainService {

    private final MetricDefinitionKeyDomainService keys;

    private static final Set<Signal> DYNAMIC_SIGNALS = Set.of(Signal.LOGS, Signal.ANALYTICS);

    /**
     * @param builtInNamesByRawKey keys read under a built-in name, with those names
     * @param builtInSpecs         the built-in filter catalog, which labels and scopes the built-in entries
     * @param builtInMetrics       the analytics definition, which labels and scopes the built-ins the filter catalog
     *                             does not describe: metrics, and facets of those metrics
     * @param readsTraffic         whether the caller may see discovered keys and {@code lastSeen}
     */
    public AdditionalMetricCatalog merge(
        Map<String, Set<String>> builtInNamesByRawKey,
        Collection<FilterSpec> builtInSpecs,
        Collection<MetricDefinition> builtInMetrics,
        List<AdditionalMetricDefinition> records,
        AdditionalMetricDiscovery discovery,
        boolean readsTraffic
    ) {
        Map<String, FilterSpec> specsByName = builtInSpecs
            .stream()
            .collect(Collectors.toMap(FilterSpec::name, Function.identity(), (a, b) -> a));
        Map<String, Description> definedByName = describe(builtInMetrics);

        List<AdditionalMetricCatalogEntry> entries = new ArrayList<>();
        for (Resolved key : resolve(builtInNamesByRawKey, records, readsTraffic ? Optional.of(discovery) : Optional.empty())) {
            switch (key.origin()) {
                case BUILT_IN -> builtInEntry(key.field(), key.builtInNames(), specsByName, definedByName, key.lastSeen()).ifPresent(
                    entries::add
                );
                case DECLARED -> entries.add(declaredEntry(key.record(), key.lastSeen()));
                case DISCOVERED -> entries.add(discoveredEntry(key.field(), key.lastSeen()));
            }
        }
        return new AdditionalMetricCatalog(entries, discovery.refreshedAt(), readsTraffic && discovery.truncated());
    }

    /** The keys listed under another origin than {@code DISCOVERED}: they neither count against the cap nor are left out. */
    public Set<String> keysListedElsewhere(Map<String, Set<String>> builtInNamesByRawKey, List<AdditionalMetricDefinition> records) {
        Set<String> keys = new HashSet<>(builtInNamesByRawKey.keySet());
        records.forEach(record -> keys.add(record.field().rawKey()));
        return keys;
    }

    /**
     * Whether a narrowing admits declared and discovered entries: they serve the logs and analytics signals and every API
     * kind but the decisions screen ({@link ApiType#admitsUnscoped}).
     */
    public boolean admitsDynamicEntries(Set<Signal> signals, Set<ApiType> apiTypes) {
        return intersects(signals, DYNAMIC_SIGNALS) && ApiType.admitsUnscoped(apiTypes);
    }

    /**
     * The filter catalog of the environment, one entry per key:
     * <ul>
     *   <li>the built-in specs, those reading an additional metric flagged {@code BUILT_IN} with its key and value type;</li>
     *   <li>the exposed records of a filterable value type, as {@code DECLARED} entries;</li>
     *   <li>for a caller who reads the environment's traffic, the other discovered keys of a filterable value type, as
     *       {@code DISCOVERED} entries labelled from their key.</li>
     * </ul>
     * <p>{@code lastSeen} is served on every flagged entry, to a traffic reader only. Declared and discovered entries
     * follow {@link #admitsDynamicEntries}. A declared key keeps its origin whatever the discovery finds: a record that
     * is not exposed hides its key from the catalog.
     *
     * @param discovery the environment's discovery, empty when the caller may not read its traffic
     */
    public List<FilterSpec> filterCatalog(
        List<FilterSpec> builtInSpecs,
        Map<String, Set<String>> builtInNamesByRawKey,
        List<AdditionalMetricDefinition> records,
        Optional<AdditionalMetricDiscovery> discovery,
        Set<Signal> signals,
        Set<ApiType> apiTypes
    ) {
        List<Resolved> resolved = resolve(builtInNamesByRawKey, records, discovery);
        Map<String, Resolved> builtInByName = new HashMap<>();
        resolved
            .stream()
            .filter(key -> key.origin() == CatalogOrigin.BUILT_IN)
            .forEach(key -> key.builtInNames().forEach(name -> builtInByName.putIfAbsent(name, key)));
        List<FilterSpec> catalog = new ArrayList<>();
        builtInSpecs.forEach(spec ->
            catalog.add(
                Optional.ofNullable(builtInByName.get(spec.name()))
                    .map(key -> spec.withCatalog(flags(key)))
                    .orElse(spec)
            )
        );
        if (!admitsDynamicEntries(signals, apiTypes)) {
            return catalog;
        }
        for (Resolved key : resolved) {
            if (key.field().valueType().isDetailOnly()) {
                continue;
            }
            switch (key.origin()) {
                case BUILT_IN -> {}
                case DECLARED -> {
                    if (key.record().exposed()) {
                        catalog.add(dynamicSpec(key, key.record().effectiveLabel()));
                    }
                }
                case DISCOVERED -> catalog.add(dynamicSpec(key, key.field().derivedLabel()));
            }
        }
        return catalog;
    }

    /**
     * One key of the environment, its origin resolved {@code BUILT_IN} › {@code DECLARED} › {@code DISCOVERED}.
     *
     * @param builtInNames the built-in names reading the key, on a {@code BUILT_IN} key
     * @param record       the record of a {@code DECLARED} key
     */
    private record Resolved(
        AdditionalMetricField field,
        CatalogOrigin origin,
        Set<String> builtInNames,
        AdditionalMetricDefinition record,
        Instant lastSeen
    ) {}

    /**
     * Every key once: the keys built-ins read, by key, then the records, then the discovered keys of the grammar that
     * are not reserved, when a discovery is given. A key a built-in reads is never declared nor discovered, a declared
     * key never discovered. {@code lastSeen} comes from the discovery. A built-in key outside the grammar yields no
     * entry but stays taken.
     */
    private List<Resolved> resolve(
        Map<String, Set<String>> builtInNamesByRawKey,
        List<AdditionalMetricDefinition> records,
        Optional<AdditionalMetricDiscovery> discovery
    ) {
        Map<String, Instant> lastSeen = discovery.map(AdditionalMetricCatalogDomainService::lastSeen).orElse(Map.of());
        List<Resolved> resolved = new ArrayList<>();
        Set<String> taken = new HashSet<>(builtInNamesByRawKey.keySet());
        new TreeMap<>(builtInNamesByRawKey).forEach((rawKey, names) ->
            keys
                .fieldOfKey(rawKey)
                .ifPresent(field -> resolved.add(new Resolved(field, CatalogOrigin.BUILT_IN, names, null, lastSeen.get(rawKey))))
        );
        for (AdditionalMetricDefinition record : records) {
            if (taken.add(record.field().rawKey())) {
                resolved.add(new Resolved(record.field(), CatalogOrigin.DECLARED, Set.of(), record, lastSeen.get(record.field().rawKey())));
            }
        }
        discovery
            .stream()
            .flatMap(found -> found.fields().stream())
            .filter(discovered -> taken.add(discovered.rawKey()))
            .flatMap(discovered -> keys.discovered(discovered.rawKey(), discovered.valueType()).stream())
            .filter(field -> !field.isReserved())
            .forEach(field -> resolved.add(new Resolved(field, CatalogOrigin.DISCOVERED, Set.of(), null, lastSeen.get(field.rawKey()))));
        return resolved;
    }

    /** An axis passes when nothing is requested on it, or when the request shares a value with {@code offered}. */
    private static <T> boolean intersects(Set<T> requested, Set<T> offered) {
        return requested == null || requested.isEmpty() || requested.stream().anyMatch(offered::contains);
    }

    private static Map<String, Instant> lastSeen(AdditionalMetricDiscovery discovery) {
        return discovery
            .fields()
            .stream()
            .collect(Collectors.toMap(DiscoveredAdditionalMetric::rawKey, DiscoveredAdditionalMetric::lastSeen, (a, b) -> a));
    }

    private static CatalogFlags flags(Resolved key) {
        return new CatalogFlags(key.origin(), key.field().rawKey(), key.field().valueType(), key.lastSeen(), true);
    }

    private static FilterSpec dynamicSpec(Resolved key, String label) {
        return new FilterSpec(
            key.field().name(),
            label,
            key.field().valueType().filterType().orElseThrow(),
            operators(key.field().valueType()),
            null,
            null,
            DYNAMIC_SIGNALS,
            Set.of(),
            flags(key)
        );
    }

    private static List<FilterOperator> operators(AdditionalMetricValueType valueType) {
        return switch (valueType) {
            case KEYWORD -> List.of(FilterOperator.EQ, FilterOperator.IN);
            case LONG, INT, DOUBLE -> List.of(FilterOperator.EQ, FilterOperator.GTE, FilterOperator.LTE);
            case BOOL -> List.of(FilterOperator.EQ);
            case STRING, JSON -> List.of();
        };
    }

    /**
     * A key read through a constant only has no built-in name to serve it under: it is listed nowhere, but stays
     * taken so no discovered entry duplicates it.
     */
    private static Optional<AdditionalMetricCatalogEntry> builtInEntry(
        AdditionalMetricField field,
        Set<String> names,
        Map<String, FilterSpec> specsByName,
        Map<String, Description> definedByName,
        Instant lastSeen
    ) {
        if (names.isEmpty()) {
            return Optional.empty();
        }
        List<String> sortedNames = names.stream().sorted().toList();
        Optional<Description> description = sortedNames
            .stream()
            .map(name -> describe(name, specsByName, definedByName))
            .flatMap(Optional::stream)
            .findFirst();
        return Optional.of(
            new AdditionalMetricCatalogEntry(
                description.map(Description::name).orElse(sortedNames.getFirst()),
                field,
                CatalogOrigin.BUILT_IN,
                description.map(Description::label).orElseGet(field::derivedLabel),
                null,
                null,
                true,
                lastSeen,
                description.map(Description::type).orElse(null),
                description.map(Description::apiTypes).orElse(Set.of())
            )
        );
    }

    /** The filter catalog describes a built-in name first; the analytics definition the names it does not. */
    private static Optional<Description> describe(
        String name,
        Map<String, FilterSpec> specsByName,
        Map<String, Description> definedByName
    ) {
        return Optional.ofNullable(specsByName.get(name))
            .map(spec -> new Description(spec.name(), spec.label(), spec.type(), spec.apiTypes()))
            .or(() -> Optional.ofNullable(definedByName.get(name)));
    }

    /**
     * The metrics of the analytics definition and the facets they are grouped by, under the definition's labels. A
     * name served for several API kinds is scoped to all of them.
     */
    private static Map<String, Description> describe(Collection<MetricDefinition> metrics) {
        Map<String, Description> byName = new HashMap<>();
        for (MetricDefinition metric : metrics) {
            Set<ApiType> apiTypes = Set.copyOf(metric.apiTypes());
            byName.merge(metric.name(), new Description(metric.name(), metric.label(), null, apiTypes), Description::widen);
            metric
                .facets()
                .forEach(facet ->
                    byName.merge(facet.name(), new Description(facet.name(), facet.label(), null, apiTypes), Description::widen)
                );
        }
        return byName;
    }

    /** How a built-in name is served: its label, its filter type when a filter serves it, its API kinds. */
    private record Description(String name, String label, FilterType type, Set<ApiType> apiTypes) {
        Description widen(Description other) {
            Set<ApiType> union = new HashSet<>(apiTypes);
            union.addAll(other.apiTypes);
            return new Description(name, label, type, union);
        }
    }

    private static AdditionalMetricCatalogEntry declaredEntry(AdditionalMetricDefinition record, Instant lastSeen) {
        return new AdditionalMetricCatalogEntry(
            record.field().name(),
            record.field(),
            CatalogOrigin.DECLARED,
            record.effectiveLabel(),
            record.description(),
            record.unit(),
            record.exposed(),
            lastSeen,
            null,
            Set.of()
        );
    }

    private static AdditionalMetricCatalogEntry discoveredEntry(AdditionalMetricField field, Instant lastSeen) {
        return new AdditionalMetricCatalogEntry(
            field.name(),
            field,
            CatalogOrigin.DISCOVERED,
            field.derivedLabel(),
            null,
            null,
            true,
            lastSeen,
            null,
            Set.of()
        );
    }
}
