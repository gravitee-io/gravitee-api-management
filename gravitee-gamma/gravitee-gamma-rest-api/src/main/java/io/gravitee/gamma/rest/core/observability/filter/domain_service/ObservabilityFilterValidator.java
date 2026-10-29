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
package io.gravitee.gamma.rest.core.observability.filter.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.gamma.rest.core.observability.filter.exception.UnsupportedObservabilityFilterException;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterCondition;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterOperator;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterSpec;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterType;
import io.gravitee.gamma.rest.core.observability.filter.model.Signal;
import io.gravitee.gamma.rest.core.observability.filter.model.StaticFilters;
import io.gravitee.gamma.rest.core.observability.filter.port.service_provider.FilterRegistry;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/**
 * Validates incoming {@link FilterCondition}s against the unified filter catalog for a given
 * {@link Signal}, and an additional metric field against the spec its name types
 * ({@link FilterRegistry#specFor}). Centralising this here keeps the registry the single source of truth and guarantees
 * that the logs and analytics surfaces enforce the <em>same</em> name / signal / operator rules for a
 * given filter — a filter and its operators behave identically whichever signal queries it.
 *
 * <p>Each condition is rejected (HTTP 400 via {@link UnsupportedObservabilityFilterException}) when:
 * <ul>
 *   <li>its {@code name} is neither in the catalog nor a well-formed additional metric field name,</li>
 *   <li>the catalog entry does not apply to the requested signal,</li>
 *   <li>its {@code operator} is not among the operators the catalog advertises for that filter,</li>
 *   <li>it carries no value, or several values on an operator that takes one, or</li>
 *   <li>a value is not one the filter accepts: blank, an unadvertised ENUM value, a non-number or an
 *       out-of-range number, or a BOOLEAN value other than {@code true} or {@code false}.</li>
 * </ul>
 *
 * <p>An analytics request ANDs every condition, the same filter included: a widget's own filters are
 * appended to the dashboard's, and conditions that cannot overlap answer an empty result. Any other
 * request names each filter once, the only repetition being a {@code GTE} and an {@code LTE} on the
 * same NUMBER filter, a closed range read as AND: the logs search would OR anything else.
 *
 * @author GraviteeSource Team
 */
@DomainService
@RequiredArgsConstructor
public class ObservabilityFilterValidator {

    private static final Set<FilterOperator> MULTI_VALUE_OPERATORS = EnumSet.of(FilterOperator.IN, FilterOperator.NOT_IN);
    private static final Set<FilterOperator> CLOSED_RANGE = EnumSet.of(FilterOperator.GTE, FilterOperator.LTE);
    private static final Set<String> BOOLEAN_VALUES = Set.of("true", "false");

    private final FilterRegistry filterRegistry;

    public void validate(List<FilterCondition> conditions, Signal signal) {
        if (conditions == null) {
            return;
        }
        Map<String, FilterSpec> specsByName = specsByName();
        for (FilterCondition condition : conditions) {
            FilterSpec spec = applicableSpec(condition, signal, specsByName);
            // No value narrows nothing, and every translator downstream skips a clause it cannot
            // build: the caller would get the unfiltered set back under an active filter chip. Holds
            // for every type, since every advertised operator takes at least one value.
            if (condition.values() == null || condition.values().isEmpty()) {
                throw UnsupportedObservabilityFilterException.blankValue(condition.name());
            }
            validateValues(condition, spec);
        }
        if (signal != Signal.ANALYTICS) {
            validateRepetition(conditions, specsByName);
        }
    }

    /**
     * The save-time check of a dashboard's own filters: refuses what every widget query would refuse later.
     * A dashboard renders analytics widgets, so its filters are checked against the ANALYTICS signal. Two
     * differences with a search, both from how the library reads a dashboard (OBS-52): a filter with no value
     * is the open "Any" slot a reader fills, accepted whatever its {@code editable} flag says; and a field is
     * filtered once, since the library keys its chips by field.
     *
     * <p>The library's JSON tab accepts a name its catalog does not know, because that catalog may be partial
     * or missing; the server holds the whole catalog, so here an unknown name is refused, but for a well-formed
     * additional metric field name, which no catalog lists.
     */
    public void validateDashboardFilters(List<FilterCondition> conditions) {
        Map<String, FilterSpec> specsByName = specsByName();
        for (FilterCondition condition : conditions) {
            FilterSpec spec = applicableSpec(condition, Signal.ANALYTICS, specsByName);
            if (condition.values() != null && !condition.values().isEmpty()) {
                validateValues(condition, spec);
            }
        }
        validateOneFilterPerField(conditions);
    }

    /**
     * Whether a condition or a facet on {@code name} reads the calls themselves: a catalog field holding a person or a
     * client identifier, or an additional metric field whose key holds the values of one.
     */
    public boolean identifiesCaller(String name) {
        return StaticFilters.identifying().contains(name) || filterRegistry.identifiesCaller(name);
    }

    private Map<String, FilterSpec> specsByName() {
        return filterRegistry
            .getFilters(Set.of(), Set.of())
            .stream()
            .collect(Collectors.toMap(FilterSpec::name, spec -> spec, (a, b) -> b));
    }

    private FilterSpec specOf(String name, Map<String, FilterSpec> specsByName) {
        var spec = specsByName.get(name);
        if (spec != null) {
            return spec;
        }
        return filterRegistry.specFor(name).orElseThrow(() -> UnsupportedObservabilityFilterException.unknownName(name));
    }

    private FilterSpec applicableSpec(FilterCondition condition, Signal signal, Map<String, FilterSpec> specsByName) {
        FilterSpec spec = specOf(condition.name(), specsByName);
        if (!spec.signals().contains(signal)) {
            throw UnsupportedObservabilityFilterException.signalMismatch(condition.name(), signal);
        }
        if (!spec.operators().contains(condition.operator())) {
            throw UnsupportedObservabilityFilterException.unsupportedOperator(condition.name(), condition.operator().name());
        }
        return spec;
    }

    private static void validateValues(FilterCondition condition, FilterSpec spec) {
        validateArity(condition);
        if (spec.type() == FilterType.STRING && hasOnlyBlankValues(condition)) {
            throw UnsupportedObservabilityFilterException.blankValue(condition.name());
        }
        validateEnumValues(condition, spec);
        validateNumberValues(condition, spec);
        validateBooleanValues(condition, spec);
    }

    private static void validateOneFilterPerField(List<FilterCondition> conditions) {
        conditions
            .stream()
            .collect(Collectors.groupingBy(FilterCondition::name, LinkedHashMap::new, Collectors.counting()))
            .forEach((name, count) -> {
                if (count > 1) {
                    throw UnsupportedObservabilityFilterException.repeatedOnDashboard(name, count);
                }
            });
    }

    // A single-value operator handed several values was read as its first value by analytics and as IN by
    // logs: refusing it is the only answer both signals can give.
    private static void validateArity(FilterCondition condition) {
        if (!MULTI_VALUE_OPERATORS.contains(condition.operator()) && condition.values().size() != 1) {
            throw UnsupportedObservabilityFilterException.invalidArity(condition.name(), condition.operator().name(), condition.values());
        }
    }

    private static void validateNumberValues(FilterCondition condition, FilterSpec spec) {
        if (spec.type() != FilterType.NUMBER) {
            return;
        }
        for (String value : condition.values()) {
            var number = parseNumber(condition.name(), value, spec);
            var range = spec.range();
            if (range != null && (isBelow(number, range.min()) || isAbove(number, range.max()))) {
                throw UnsupportedObservabilityFilterException.valueOutOfRange(condition.name(), value, range.min(), range.max());
            }
        }
    }

    // A NUMBER filter counts or measures in whole units, and the logs translator parses them as such; only a
    // double_ additional metric field takes decimals.
    private static BigDecimal parseNumber(String filterName, String value, FilterSpec spec) {
        try {
            return spec.takesDecimals() ? new BigDecimal(value.trim()) : BigDecimal.valueOf(Long.parseLong(value.trim()));
        } catch (NumberFormatException e) {
            throw UnsupportedObservabilityFilterException.invalidNumber(
                filterName,
                value,
                spec.takesDecimals() ? "a number" : "a whole number"
            );
        }
    }

    private static boolean isBelow(BigDecimal number, Number min) {
        return min != null && number.compareTo(new BigDecimal(min.toString())) < 0;
    }

    private static boolean isAbove(BigDecimal number, Number max) {
        return max != null && number.compareTo(new BigDecimal(max.toString())) > 0;
    }

    // A boolean field is indexed as such: a value other than true or false would match nothing, not fail.
    private static void validateBooleanValues(FilterCondition condition, FilterSpec spec) {
        if (spec.type() != FilterType.BOOLEAN) {
            return;
        }
        for (String value : condition.values()) {
            if (!BOOLEAN_VALUES.contains(value)) {
                throw UnsupportedObservabilityFilterException.unknownEnumValue(condition.name(), value);
            }
        }
    }

    private void validateRepetition(List<FilterCondition> conditions, Map<String, FilterSpec> specsByName) {
        conditions
            .stream()
            .collect(Collectors.groupingBy(FilterCondition::name, LinkedHashMap::new, Collectors.toList()))
            .forEach((name, repeated) -> {
                if (repeated.size() == 1) {
                    return;
                }
                var operators = repeated.stream().map(FilterCondition::operator).toList();
                var spec = specOf(name, specsByName);
                if (!isClosedRange(spec, operators)) {
                    throw UnsupportedObservabilityFilterException.repeated(name, operators.stream().map(FilterOperator::name).toList());
                }
                validateRangeBounds(name, repeated, spec);
            });
    }

    private static boolean isClosedRange(FilterSpec spec, List<FilterOperator> operators) {
        return spec.type() == FilterType.NUMBER && operators.size() == 2 && Set.copyOf(operators).equals(CLOSED_RANGE);
    }

    private static void validateRangeBounds(String name, List<FilterCondition> range, FilterSpec spec) {
        var lower = boundOf(name, range, FilterOperator.GTE, spec);
        var upper = boundOf(name, range, FilterOperator.LTE, spec);
        if (lower.compareTo(upper) > 0) {
            throw UnsupportedObservabilityFilterException.invertedRange(name, lower, upper);
        }
    }

    private static BigDecimal boundOf(String name, List<FilterCondition> range, FilterOperator operator, FilterSpec spec) {
        return range
            .stream()
            .filter(condition -> condition.operator() == operator)
            .map(condition -> parseNumber(name, condition.values().getFirst(), spec))
            .findFirst()
            .orElseThrow();
    }

    /**
     * ENUM conditions must only carry advertised values: downstream translators silently drop
     * clauses they cannot express (e.g. an unknown failure origin), which would turn a typo into
     * an unfiltered result set instead of a 400.
     */
    private static void validateEnumValues(FilterCondition condition, FilterSpec spec) {
        if (spec.type() != FilterType.ENUM || spec.enumValues() == null || spec.enumValues().isEmpty()) {
            return;
        }
        var allowed = spec.enumValues().stream().map(FilterSpec.EnumValue::value).collect(Collectors.toSet());
        for (String value : condition.values()) {
            if (!allowed.contains(value)) {
                throw UnsupportedObservabilityFilterException.unknownEnumValue(condition.name(), value);
            }
        }
    }

    private static boolean hasOnlyBlankValues(FilterCondition condition) {
        return (
            condition.values() == null ||
            condition.values().isEmpty() ||
            condition
                .values()
                .stream()
                .allMatch(value -> value == null || value.isBlank())
        );
    }
}
