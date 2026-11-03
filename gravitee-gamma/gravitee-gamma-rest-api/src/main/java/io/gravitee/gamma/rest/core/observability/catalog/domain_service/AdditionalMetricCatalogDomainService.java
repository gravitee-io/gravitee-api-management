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
        Map<String, Instant> lastSeen = readsTraffic
            ? discovery
                .fields()
                .stream()
                .collect(Collectors.toMap(DiscoveredAdditionalMetric::rawKey, DiscoveredAdditionalMetric::lastSeen, (a, b) -> a))
            : Map.of();
        Map<String, FilterSpec> specsByName = builtInSpecs
            .stream()
            .collect(Collectors.toMap(FilterSpec::name, Function.identity(), (a, b) -> a));
        Map<String, Description> definedByName = describe(builtInMetrics);

        List<AdditionalMetricCatalogEntry> entries = new ArrayList<>();
        Set<String> taken = new HashSet<>(builtInNamesByRawKey.keySet());
        new TreeMap<>(builtInNamesByRawKey).forEach((rawKey, names) ->
            builtInEntry(rawKey, names, specsByName, definedByName, lastSeen.get(rawKey)).ifPresent(entries::add)
        );
        for (AdditionalMetricDefinition record : records) {
            if (taken.add(record.field().rawKey())) {
                entries.add(declaredEntry(record, lastSeen.get(record.field().rawKey())));
            }
        }
        if (readsTraffic) {
            discovery
                .fields()
                .stream()
                .filter(discovered -> taken.add(discovered.rawKey()))
                .flatMap(discovered -> keys.discovered(discovered.rawKey(), discovered.valueType()).stream())
                .filter(field -> !field.isReserved())
                .map(field -> discoveredEntry(field, lastSeen.get(field.rawKey())))
                .forEach(entries::add);
        }
        return new AdditionalMetricCatalog(entries, discovery.refreshedAt(), readsTraffic && discovery.truncated());
    }

    /**
     * The records the filter catalog offers: exposed ones of a filterable value type, as {@code DECLARED} entries of
     * the logs and analytics signals. A record is tied to no API kind: a narrowing on API kinds keeps it for any kind
     * and drops it for the decisions screen ({@code AUTHZ_DECISION}) alone. A key a built-in reads is left to the
     * built-in entry. {@code lastSeen} is not served here.
     */
    public List<FilterSpec> declaredFilterSpecs(
        List<AdditionalMetricDefinition> records,
        Set<String> builtInRawKeys,
        Set<Signal> signals,
        Set<ApiType> apiTypes
    ) {
        if (!intersects(signals, DYNAMIC_SIGNALS) || !intersects(apiTypes, ApiType.API_KINDS)) {
            return List.of();
        }
        return records
            .stream()
            .filter(AdditionalMetricDefinition::exposed)
            .filter(record -> !record.field().valueType().isDetailOnly())
            .filter(record -> !builtInRawKeys.contains(record.field().rawKey()))
            .map(AdditionalMetricCatalogDomainService::filterSpec)
            .toList();
    }

    /** An axis passes when nothing is requested on it, or when the request shares a value with {@code offered}. */
    private static <T> boolean intersects(Set<T> requested, Set<T> offered) {
        return requested == null || requested.isEmpty() || requested.stream().anyMatch(offered::contains);
    }

    private static FilterSpec filterSpec(AdditionalMetricDefinition record) {
        AdditionalMetricField field = record.field();
        return new FilterSpec(
            field.name(),
            record.effectiveLabel(),
            field.valueType().filterType().orElseThrow(),
            operators(field.valueType()),
            null,
            null,
            DYNAMIC_SIGNALS,
            Set.of(),
            new CatalogFlags(CatalogOrigin.DECLARED, field.rawKey(), field.valueType(), null, true)
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
        String rawKey,
        Set<String> names,
        Map<String, FilterSpec> specsByName,
        Map<String, Description> definedByName,
        Instant lastSeen
    ) {
        Optional<AdditionalMetricValueType> valueType = AdditionalMetricValueType.ofRawKey(rawKey);
        if (names.isEmpty() || valueType.isEmpty()) {
            return Optional.empty();
        }
        AdditionalMetricField field = new AdditionalMetricField(rawKey, valueType.get());
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
