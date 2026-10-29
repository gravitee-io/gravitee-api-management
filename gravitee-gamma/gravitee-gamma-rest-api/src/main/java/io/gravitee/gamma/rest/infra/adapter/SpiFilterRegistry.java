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
package io.gravitee.gamma.rest.infra.adapter;

import io.gravitee.apim.core.observability.model.AdditionalMetricField;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricValueType;
import io.gravitee.gamma.rest.core.observability.catalog.model.CatalogFlags;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.core.observability.filter.model.CommonFilters;
import io.gravitee.gamma.rest.core.observability.filter.model.ExtensibleFilters;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterOperator;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterSpec;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterType;
import io.gravitee.gamma.rest.core.observability.filter.model.Signal;
import io.gravitee.gamma.rest.core.observability.filter.model.StaticFilters;
import io.gravitee.gamma.rest.core.observability.filter.port.service_provider.FilterContributor;
import io.gravitee.gamma.rest.core.observability.filter.port.service_provider.FilterRegistry;
import io.gravitee.node.logging.NodeLoggerFactory;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Predicate;
import org.slf4j.Logger;

/**
 * {@link FilterRegistry} adapter that assembles the catalog from two sources:
 * <ol>
 *   <li>the <b>host catalog</b> — {@link StaticFilters} (fixed) + {@link ExtensibleFilters} (shells
 *       seeded with their baseline values), built from the enums directly;</li>
 *   <li>the <b>contributors</b> — each adds new {@link FilterContributor#filters()} and/or extra
 *       values for extensible filters via {@link FilterContributor#enumValues()}.</li>
 * </ol>
 *
 * <p>Merge rules: a contributor filter whose name is host-owned ({@link CommonFilters#names()}) is
 * rejected (warn); two contributors shipping the same name → last-write-wins with a descriptive warn
 * on differing definitions; extensible filter values are the union (dedup by value) of baseline +
 * contributions; an extensible filter that ends up with no values is omitted.
 *
 * <p>Query-time filtering applies two independent axes ({@link FilterSpec#signals()} and
 * {@link FilterSpec#apiTypes()}) by set intersection; an absent/empty axis is unconstrained.
 *
 * <p>Contributors are sourced via {@link ServiceLoader} (host classpath only) unless a test passes its own.
 * Cross-plugin module contributions require the separate plugin-handler registration mechanism.
 *
 * @author GraviteeSource Team
 */
public class SpiFilterRegistry implements FilterRegistry {

    private static final Logger LOGGER = NodeLoggerFactory.getLogger(SpiFilterRegistry.class);

    private final Map<String, FilterSpec> specsByName;
    private final Predicate<AdditionalMetricField> identifyingField;

    /** @param identifyingField whether an additional metric field holds the values of a built-in identifying a caller */
    public SpiFilterRegistry(Predicate<AdditionalMetricField> identifyingField) {
        this(contributorsOnClasspath(), identifyingField);
    }

    /** Test seam — without an analytics store, no additional metric field identifies a caller. */
    public SpiFilterRegistry() {
        this(contributorsOnClasspath(), _ -> false);
    }

    /** Test seam — inject a fixed contributor list without touching the classpath. */
    public SpiFilterRegistry(List<FilterContributor> contributors) {
        this(contributors, _ -> false);
    }

    private SpiFilterRegistry(List<FilterContributor> contributors, Predicate<AdditionalMetricField> identifyingField) {
        this.specsByName = assemble(contributors);
        this.identifyingField = identifyingField;
    }

    private static List<FilterContributor> contributorsOnClasspath() {
        return ServiceLoader.load(FilterContributor.class).stream().map(ServiceLoader.Provider::get).toList();
    }

    private static Map<String, FilterSpec> assemble(List<FilterContributor> contributors) {
        Map<String, FilterSpec> byName = new LinkedHashMap<>();

        // 1. Host static filters.
        for (StaticFilters staticFilter : StaticFilters.values()) {
            byName.put(staticFilter.filterName(), staticFilter.toSpec());
        }

        // 2. Host extensible filters, seeded with their baseline values (re-assembled in step 4).
        Map<ExtensibleFilters, LinkedHashMap<String, FilterSpec.EnumValue>> extensibleValues = new EnumMap<>(ExtensibleFilters.class);
        for (ExtensibleFilters extensible : ExtensibleFilters.values()) {
            LinkedHashMap<String, FilterSpec.EnumValue> values = new LinkedHashMap<>();
            for (FilterSpec.EnumValue value : extensible.baselineValues()) {
                values.put(value.value(), value);
            }
            extensibleValues.put(extensible, values);
            byName.put(extensible.filterName(), extensible.toSpec(List.copyOf(values.values())));
        }

        // 3. Contributors: new filters + extra values for extensible filters.
        Set<String> hostOwned = CommonFilters.names();
        for (FilterContributor contributor : contributors) {
            addContributorFilters(byName, hostOwned, contributor);
            mergeContributorValues(extensibleValues, contributor);
        }

        // 4. Re-assemble extensible filters with their merged values (omit if empty).
        for (ExtensibleFilters extensible : ExtensibleFilters.values()) {
            LinkedHashMap<String, FilterSpec.EnumValue> values = extensibleValues.get(extensible);
            if (values.isEmpty()) {
                byName.remove(extensible.filterName());
            } else {
                byName.put(extensible.filterName(), extensible.toSpec(List.copyOf(values.values())));
            }
        }

        return Collections.unmodifiableMap(byName);
    }

    private static void addContributorFilters(Map<String, FilterSpec> byName, Set<String> hostOwned, FilterContributor contributor) {
        for (FilterSpec spec : contributor.filters()) {
            if (hostOwned.contains(spec.name())) {
                LOGGER.warn("A filter contributor tried to (re)define host-owned filter '{}'; ignored.", spec.name());
                continue;
            }
            FilterSpec previous = byName.put(spec.name(), spec);
            if (previous != null && !previous.equals(spec)) {
                LOGGER.warn(
                    "Duplicate filter '{}' from multiple contributors with differing definitions (signals {} vs {}, apiTypes {} vs {}, operators {} vs {}); last one wins, coverage from the dropped one is lost.",
                    spec.name(),
                    previous.signals(),
                    spec.signals(),
                    previous.apiTypes(),
                    spec.apiTypes(),
                    previous.operators(),
                    spec.operators()
                );
            }
        }
    }

    private static void mergeContributorValues(
        Map<ExtensibleFilters, LinkedHashMap<String, FilterSpec.EnumValue>> extensibleValues,
        FilterContributor contributor
    ) {
        contributor
            .enumValues()
            .forEach((extensible, values) -> {
                if (values == null) {
                    return;
                }
                LinkedHashMap<String, FilterSpec.EnumValue> target = extensibleValues.get(extensible);
                for (FilterSpec.EnumValue value : values) {
                    target.putIfAbsent(value.value(), value); // union, first label wins
                }
            });
    }

    @Override
    public List<FilterSpec> getFilters(Set<Signal> signals, Set<ApiType> apiTypes) {
        return specsByName
            .values()
            .stream()
            .filter(spec -> matches(spec.signals(), signals))
            .filter(spec -> matches(spec.apiTypes(), apiTypes))
            .toList();
    }

    @Override
    public Optional<FilterSpec> specFor(String name) {
        return Optional.ofNullable(specsByName.get(name)).or(() -> AdditionalMetricField.parse(name).map(SpiFilterRegistry::specOf));
    }

    @Override
    public boolean identifiesCaller(String name) {
        return AdditionalMetricField.parse(name).map(identifyingField::test).orElse(false);
    }

    /**
     * The validation spec of an additional metric field, typed by its prefix (OBS-70, §2). No label: deriving one here
     * would be a second label rule. Analytics only: OBS-76 adds LOGS to its {@code signals} here.
     */
    private static FilterSpec specOf(AdditionalMetricField field) {
        var type = switch (field.valueType()) {
            case KEYWORD -> FilterType.KEYWORD;
            case LONG, INT, DOUBLE -> FilterType.NUMBER;
            case BOOL -> FilterType.BOOLEAN;
            case STRING, JSON -> FilterType.STRING;
        };
        var operators = field
            .valueType()
            .operators()
            .stream()
            .map(operator -> FilterOperator.valueOf(operator.name()))
            .sorted()
            .toList();
        return new FilterSpec(
            field.name(),
            null,
            type,
            operators,
            null,
            null,
            Set.of(Signal.ANALYTICS),
            Set.of(),
            new CatalogFlags(null, field.rawKey(), AdditionalMetricValueType.valueOf(field.valueType().name()), null, true)
        );
    }

    /** An axis matches when no values are requested, or when the spec's non-empty set intersects the requested one. */
    private static boolean matches(Collection<?> specAxis, Collection<?> requested) {
        if (requested == null || requested.isEmpty()) {
            return true;
        }
        return specAxis != null && !Collections.disjoint(specAxis, requested);
    }
}
