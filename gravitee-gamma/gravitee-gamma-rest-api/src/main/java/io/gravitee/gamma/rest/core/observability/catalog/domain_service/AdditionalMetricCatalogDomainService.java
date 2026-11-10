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
import io.gravitee.gamma.rest.core.observability.analytics.model.AdditionalMetricSupport;
import io.gravitee.gamma.rest.core.observability.analytics.model.DimensionDefinition;
import io.gravitee.gamma.rest.core.observability.analytics.model.MetricDefinition;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricCatalog;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricCatalogEntry;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricDefinition;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricDiscovery;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricField;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricLabel;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricNamespace;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;

/**
 * Merges the three origins of the environment's additional metrics into one entry per key, resolved
 * {@code BUILT_IN} › {@code DECLARED} › {@code DISCOVERED}, with the label precedence declared › built-in › derived.
 * A declared or discovered key is tied to the API kinds its namespace names ({@link AdditionalMetricNamespace}) and
 * served for them; a key of no known namespace is served for every kind.
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
                case DECLARED, DISCOVERED -> entries.add(dynamicEntry(key));
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
     *   <li>the numeric keys a built-in metric reads and no built-in filter reads, as {@code BUILT_IN} entries under
     *       their dynamic name, labelled and scoped by the metric ({@link #builtInKeyFilter});</li>
     *   <li>the exposed records of a filterable value type, as {@code DECLARED} entries;</li>
     *   <li>for a caller who reads the environment's traffic, the other discovered keys of a filterable value type, as
     *       {@code DISCOVERED} entries labelled from their key.</li>
     * </ul>
     * <p>{@code lastSeen} is served on every flagged entry, to a traffic reader only. Declared and discovered entries
     * follow {@link #admitsDynamicEntries}, then each answers the kinds asked as a built-in does: offered for the kinds
     * its namespace ties it to, it is kept when the request shares one; tied to no kind, it is kept for any API kind.
     * A declared key keeps its origin whatever the discovery finds: a record that is not exposed hides its key from
     * the catalog.
     *
     * @param builtInSpecs       the built-in specs the registry serves for the narrowing
     * @param builtInFilterNames every name of the registry, whatever the narrowing: a filter narrowed out still reads
     *                           its key
     * @param builtInMetrics     the analytics definition's built-ins, every kind
     * @param discovery          the environment's discovery, empty when the caller may not read its traffic
     */
    public List<FilterSpec> filterCatalog(
        List<FilterSpec> builtInSpecs,
        Set<String> builtInFilterNames,
        Collection<MetricDefinition> builtInMetrics,
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
        Map<String, MetricDefinition> metricsByName = builtInMetrics
            .stream()
            .collect(Collectors.toMap(MetricDefinition::name, Function.identity(), (first, next) -> first));
        for (Resolved key : resolved) {
            if (key.origin() == CatalogOrigin.BUILT_IN) {
                builtInKeyFilter(key, builtInFilterNames, metricsByName)
                    .filter(spec -> intersects(signals, spec.signals()) && servedFor(spec.apiTypes(), apiTypes))
                    .ifPresent(catalog::add);
            }
        }
        if (!admitsDynamicEntries(signals, apiTypes)) {
            return catalog;
        }
        for (Resolved key : resolved) {
            if (key.listed() && !key.field().valueType().isDetailOnly() && servedFor(key.field().apiTypes(), apiTypes)) {
                catalog.add(dynamicSpec(key));
            }
        }
        return catalog;
    }

    /**
     * What the environment's keys add to the analytics definition: its numeric keys as metrics of the {@code ADDITIONAL}
     * family, its keyword and bool keys as facets, every entry carrying its namespace and the kinds it ties it to.
     */
    public record AdditionalAnalytics(List<MetricDefinition> metrics, List<DimensionDefinition> facets) {
        /**
         * The facets a metric served for {@code kinds} is grouped by: those tied to one of its kinds, and those tied
         * to none.
         */
        public List<DimensionDefinition> facetsFor(List<ApiType> kinds) {
            return facets
                .stream()
                .filter(facet -> facet.apiTypes().isEmpty() || facet.apiTypes().stream().anyMatch(kinds::contains))
                .toList();
        }
    }

    /**
     * The environment's keys as entries of the analytics definition for some API kinds, in the order {@link #resolve}
     * lists them: the exposed records as {@code DECLARED} entries then, when a discovery is given, the other discovered
     * keys as {@code DISCOVERED} entries. A key a built-in reads is never listed: the built-in is. Labels follow
     * declared › derived; the unit and the description are the record's, none otherwise.
     *
     * <p>A key is listed for the kinds its namespace ties it to: a numeric key for those among the kinds the engine
     * measures such a field for ({@code support}), so the definition never promises a kind the engine refuses, and not
     * at all when none is left; a numeric key of no known namespace for every kind the engine measures such a field
     * for, the filter catalog serving it tied to none. Every metric takes the engine's measures and filters, and is
     * grouped by the engine's facets then by the keys listed as facets for one of its kinds.
     *
     * @param discovery the environment's discovery, empty when the caller may not read its traffic
     * @param apiTypes  the kinds asked; none asks for every key
     */
    public AdditionalAnalytics analyticsCatalog(
        Map<String, Set<String>> builtInNamesByRawKey,
        List<AdditionalMetricDefinition> records,
        Optional<AdditionalMetricDiscovery> discovery,
        AdditionalMetricSupport support,
        Set<ApiType> apiTypes
    ) {
        Map<Resolved, List<ApiType>> measured = new LinkedHashMap<>();
        List<DimensionDefinition> facets = new ArrayList<>();
        for (Resolved key : resolve(builtInNamesByRawKey, records, discovery)) {
            if (!key.listed()) {
                continue;
            }
            AdditionalMetricValueType valueType = key.field().valueType();
            if (valueType.isNumber()) {
                measuredKinds(key.field(), support)
                    .filter(kinds -> servedFor(Set.copyOf(kinds), apiTypes))
                    .ifPresent(kinds -> measured.put(key, kinds));
            } else if (valueType.isFacet() && servedFor(key.field().apiTypes(), apiTypes)) {
                facets.add(
                    new DimensionDefinition(
                        key.field().name(),
                        key.label(),
                        key.description(),
                        key.namespace(),
                        key.field().apiTypes(),
                        key.lastSeen(),
                        key.origin()
                    )
                );
            }
        }
        var additional = new AdditionalAnalytics(List.of(), facets);
        List<MetricDefinition> metrics = measured
            .entrySet()
            .stream()
            .map(entry -> metric(entry.getKey(), entry.getValue(), support).groupedAlsoBy(additional.facetsFor(entry.getValue())))
            .toList();
        return new AdditionalAnalytics(metrics, additional.facets());
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
    ) {
        /** Whether the key is served as a dynamic entry: declared and exposed, or discovered. */
        boolean listed() {
            return origin == CatalogOrigin.DECLARED ? record.exposed() : origin == CatalogOrigin.DISCOVERED;
        }

        /** Declared › derived. */
        String label() {
            return record != null ? record.effectiveLabel() : field.derivedLabel();
        }

        String description() {
            return record != null ? record.description() : null;
        }

        String unit() {
            return record != null ? record.unit() : null;
        }

        /** The namespace served on a dynamic entry; a built-in is scoped by its own catalog, not by its key. */
        String namespace() {
            return origin == CatalogOrigin.BUILT_IN ? null : field.namespace().orElse(null);
        }
    }

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

    /**
     * Whether a dynamic entry answers a narrowing on {@code requested}, as the filter registry decides it for a
     * built-in: offered for some kinds, it passes when the request shares one; tied to no kind, it passes as
     * {@link ApiType#admitsUnscoped} says.
     */
    private static boolean servedFor(Set<ApiType> offered, Set<ApiType> requested) {
        return offered.isEmpty() ? ApiType.admitsUnscoped(requested) : intersects(requested, offered);
    }

    /**
     * The kinds a numeric key is measured for, in the engine's order: those its namespace ties it to among the
     * engine's, every one of the engine's when the namespace says nothing (the widget editor offers a metric for the
     * kinds it carries, so a key of no kind would be offered nowhere). Empty when the namespace ties the key to kinds
     * the engine measures no such field for: the key is then no metric of the definition.
     */
    private static Optional<List<ApiType>> measuredKinds(AdditionalMetricField field, AdditionalMetricSupport support) {
        Set<ApiType> tied = field.apiTypes();
        if (tied.isEmpty()) {
            return Optional.of(support.apiTypes());
        }
        List<ApiType> kinds = support.apiTypes().stream().filter(tied::contains).toList();
        return kinds.isEmpty() ? Optional.empty() : Optional.of(kinds);
    }

    private static Map<String, Instant> lastSeen(AdditionalMetricDiscovery discovery) {
        return discovery
            .fields()
            .stream()
            .collect(Collectors.toMap(DiscoveredAdditionalMetric::rawKey, DiscoveredAdditionalMetric::lastSeen, (a, b) -> a));
    }

    private static CatalogFlags flags(Resolved key) {
        return new CatalogFlags(key.origin(), key.field().rawKey(), key.field().valueType(), key.lastSeen(), true, key.namespace());
    }

    /**
     * A numeric key a built-in metric reads and no built-in filter reads, as a filter under its dynamic name: the
     * metric's label and kinds, {@code BUILT_IN} with the key's flags, the operators of its value type (a {@code double_}
     * key takes decimals), served on the analytics signal and, when the logs search serves one of its kinds, on the
     * logs signal. A keyword key a built-in facet reads is not a filter: nothing lists its values. Judged on every
     * name of the registry: a filter narrowed out of the request still reads the key.
     */
    private static Optional<FilterSpec> builtInKeyFilter(
        Resolved key,
        Set<String> builtInFilterNames,
        Map<String, MetricDefinition> metricsByName
    ) {
        AdditionalMetricField field = key.field();
        if (!field.valueType().isNumber() || key.builtInNames().stream().anyMatch(builtInFilterNames::contains)) {
            return Optional.empty();
        }
        return key
            .builtInNames()
            .stream()
            .sorted()
            .map(metricsByName::get)
            .filter(Objects::nonNull)
            .findFirst()
            .map(metric ->
                new FilterSpec(
                    field.name(),
                    metric.label(),
                    null,
                    field.valueType().filterType().orElseThrow(),
                    operators(field.valueType()),
                    null,
                    null,
                    signalsFor(metric.apiTypes()),
                    Set.copyOf(metric.apiTypes()),
                    flags(key)
                )
            );
    }

    /** The logs signal only for kinds the logs search serves; the analytics signal always. */
    private static Set<Signal> signalsFor(Collection<ApiType> kinds) {
        return kinds.stream().anyMatch(ApiType.SEARCHED_ON_LOGS::contains) ? DYNAMIC_SIGNALS : Set.of(Signal.ANALYTICS);
    }

    private static FilterSpec dynamicSpec(Resolved key) {
        return new FilterSpec(
            key.field().name(),
            key.label(),
            key.description(),
            key.field().valueType().filterType().orElseThrow(),
            operators(key.field().valueType()),
            null,
            null,
            DYNAMIC_SIGNALS,
            key.field().apiTypes(),
            flags(key)
        );
    }

    private static MetricDefinition metric(Resolved key, List<ApiType> apiTypes, AdditionalMetricSupport support) {
        return new MetricDefinition(
            key.field().name(),
            key.label(),
            key.description(),
            key.namespace(),
            key.unit(),
            apiTypes,
            support.measures(),
            support.facets(),
            support.filters(),
            key.origin(),
            null,
            key.lastSeen()
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
                description.map(Description::label).orElseGet(() -> AdditionalMetricLabel.deriveFromWholeKey(field)),
                null,
                null,
                true,
                lastSeen,
                description.map(Description::type).orElse(null),
                description.map(Description::apiTypes).orElse(Set.of()),
                null
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

    /** A declared or discovered key, scoped and named by its namespace; a declared key is listed exposed or not. */
    private static AdditionalMetricCatalogEntry dynamicEntry(Resolved key) {
        return new AdditionalMetricCatalogEntry(
            key.field().name(),
            key.field(),
            key.origin(),
            key.label(),
            key.description(),
            key.unit(),
            key.record() == null || key.record().exposed(),
            key.lastSeen(),
            null,
            key.field().apiTypes(),
            key.namespace()
        );
    }
}
