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
package io.gravitee.apim.infra.query_service.analytics_engine;

import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.AuditInfoFixtures;
import io.gravitee.apim.core.analytics_engine.model.AnalyticsQueryContext;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.observability.model.FilterOperator;
import io.gravitee.apim.core.observability.model.Signal;
import io.gravitee.apim.infra.adapter.AnalyticsMeasuresAdapter;
import io.gravitee.apim.infra.domain_service.analytics_engine.definition.AnalyticsDefinitionYAMLQueryService;
import io.gravitee.apim.infra.domain_service.analytics_engine.processors.ApiTypeFilterTransformer;
import io.gravitee.repository.analytics.engine.api.query.AnalyticsSearchPath;
import io.gravitee.repository.analytics.engine.api.query.Filter;
import io.gravitee.repository.analytics.engine.api.query.FilterOutcome;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter.AuthzFieldResolver;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter.AuthzTrafficFieldResolver;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter.EventMetricsFieldResolver;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter.FilterAdapter;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter.HTTPFieldResolver;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter.MessageFieldResolver;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter.NativeApiFieldResolver;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter.api.FieldResolver;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * Contract between the analytics filter catalog and the Elasticsearch adapters.
 *
 * <p>{@code FilterAdapter} keeps one allow-list per search family and drops, without a word, any condition
 * its family does not list; the {@code FieldResolver} of that family then turns a listed name into a field.
 * A catalog filter that no family applies is offered on every analytics screen and filters nothing; one that its
 * families apply but cannot resolve fails the query instead. This test loads the <b>real</b> definition file and requires every filter it
 * advertises for {@link Signal#ANALYTICS} to be both applied and resolved by at least one search path.
 *
 * <p>It lives here rather than in the Elasticsearch module because only this module knows how a catalog
 * name reaches the engine: the {@code API_TYPE} rewrite and the {@code FilterSpec} → {@code Filter} mapping
 * run in this module, and the definition file is one of its resources.
 *
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AnalyticsFilterElasticsearchCoverageTest {

    /**
     * Gaps found when the test was written, each tracked by its ticket. A name leaves this map when its gap
     * is fixed: the test fails while a listed name is covered, so the map cannot outlive the gap.
     *
     * <p>The Elasticsearch module records the same GMA-932 gap twice, in
     * {@code FilterAdapterFieldResolverParityTest.KNOWN_UNRESOLVABLE_MESSAGE_NAMES} and
     * {@code FilterAdapterOutcomeTest.KNOWN_UNRESOLVABLE_ON_MESSAGE}: the three lists shrink together.
     */
    private static final Map<Filter.Name, String> KNOWN_GAPS = Map.of(
        Filter.Name.MESSAGE_COUNT,
        "https://gravitee.atlassian.net/browse/GMA-932",
        Filter.Name.MESSAGE_ERROR_COUNT,
        "https://gravitee.atlassian.net/browse/GMA-932",
        Filter.Name.MESSAGE_SIZE,
        "https://gravitee.atlassian.net/browse/GMA-932"
    );

    private static final AnalyticsDefinitionYAMLQueryService CATALOG = new AnalyticsDefinitionYAMLQueryService();

    private static final ApiTypeFilterTransformer API_TYPE_TRANSFORMER = new ApiTypeFilterTransformer();

    private static final AnalyticsQueryContext CONTEXT = AnalyticsQueryContext.forApis(
        AuditInfoFixtures.anAuditInfo("org-id", "env-id", "user-id"),
        Set.of("api-id")
    );

    @Test
    void every_analytics_filter_of_the_catalog_should_be_applied_and_resolved_by_an_elasticsearch_search_path() {
        var analyticsFilters = analyticsFilterNames();
        assertThat(analyticsFilters).as("the catalog must advertise filters for the ANALYTICS signal").isNotEmpty();

        var gaps = new TreeMap<String, String>();
        for (var catalogName : analyticsFilters) {
            for (var engineName : engineNames(catalogName)) {
                if (!KNOWN_GAPS.containsKey(engineName)) {
                    gapOf(engineName).ifPresent(gap -> gaps.put(describe(catalogName, engineName), gap));
                }
            }
        }

        assertThat(gaps)
            .as(
                "ANALYTICS filters of analytics-definition.yaml that no Elasticsearch search both applies and resolves: " +
                    "each is offered on the analytics screens and either dropped from the query or failing it. Allow-list " +
                    "the name in its FilterAdapter family and resolve it in that family's FieldResolver, or drop ANALYTICS " +
                    "from its signals."
            )
            .isEmpty();
    }

    @Test
    void known_gaps_should_still_be_gaps_of_the_catalog() {
        var analyticsEngineNames = analyticsFilterNames()
            .stream()
            .flatMap(name -> engineNames(name).stream())
            .collect(Collectors.toSet());

        assertThat(KNOWN_GAPS.keySet())
            .as("KNOWN_GAPS lists a name the catalog no longer advertises for ANALYTICS, or one now covered: remove it")
            .allSatisfy(name -> {
                assertThat(analyticsEngineNames).contains(name);
                assertThat(gapOf(name)).isPresent();
            });
    }

    /**
     * The catalog's ANALYTICS filters, read off the {@code signals} axis: the top-level filters advertised for
     * ANALYTICS, plus the filters a metric accepts unless their declaration keeps them off ANALYTICS.
     *
     * <p>A metric filter the catalog does not declare counts as ANALYTICS, the rule
     * {@code AnalyticsQueryValidator.supportsAnalytics} applies to queries: the two move together.
     */
    private static Set<FilterSpec.Name> analyticsFilterNames() {
        var topLevel = CATALOG.getFilters(Set.of(Signal.ANALYTICS)).stream().map(FilterSpec::name);
        var ofMetrics = CATALOG.getApis()
            .stream()
            .flatMap(api -> CATALOG.getMetrics(api.name()).stream())
            .flatMap(metric -> metric.filters().stream())
            .filter(name ->
                CATALOG.findFilter(name)
                    .map(spec -> spec.appliesTo(Signal.ANALYTICS))
                    .orElse(true)
            );
        return Stream.concat(topLevel, ofMetrics).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * The engine conditions a catalog filter becomes, through the same rewrite and mapping a query goes through:
     * {@code API_TYPE} becomes an {@code API} condition, every other name maps one to one. The transformer also
     * appends the caller's {@code API} scope to every query; that condition stands for the input only when the
     * input is about APIs, so it is left out otherwise rather than checked once per catalog name.
     */
    private static List<Filter.Name> engineNames(FilterSpec.Name catalogName) {
        var condition = new io.gravitee.apim.core.analytics_engine.model.Filter(catalogName, FilterOperator.EQ, sampleValue(catalogName));
        return API_TYPE_TRANSFORMER.transform(CONTEXT, List.of(condition))
            .stream()
            .map(io.gravitee.apim.core.analytics_engine.model.Filter::name)
            .filter(name -> name != FilterSpec.Name.API || isAboutApis(catalogName))
            .map(name -> toEngineName(catalogName, name))
            .distinct()
            .toList();
    }

    private static boolean isAboutApis(FilterSpec.Name catalogName) {
        return catalogName == FilterSpec.Name.API || catalogName == FilterSpec.Name.API_TYPE;
    }

    private static Filter.Name toEngineName(FilterSpec.Name catalogName, FilterSpec.Name name) {
        try {
            return AnalyticsMeasuresAdapter.INSTANCE.toFilterName(name);
        } catch (IllegalArgumentException e) {
            throw new AssertionError(
                "%s is advertised for ANALYTICS but has no analytics engine filter name: drop ANALYTICS from its signals, or give it one".formatted(
                    catalogName
                ),
                e
            );
        }
    }

    private static String describe(FilterSpec.Name catalogName, Filter.Name engineName) {
        return catalogName.name().equals(engineName.name()) ? catalogName.name() : catalogName + " (as " + engineName + ")";
    }

    /** {@code API_TYPE} only accepts a kind it knows; any other value is enough to name the condition. */
    private static String sampleValue(FilterSpec.Name catalogName) {
        return catalogName == FilterSpec.Name.API_TYPE ? "HTTP_PROXY" : "value";
    }

    /** Why no search path covers this condition, or empty when one applies it and resolves its field. */
    private static Optional<String> gapOf(Filter.Name name) {
        var applyingPaths = Arrays.stream(AnalyticsSearchPath.values())
            .filter(path -> FilterAdapter.outcome(path, name) == FilterOutcome.APPLIED)
            .toList();
        if (applyingPaths.isEmpty()) {
            return Optional.of("in no FilterAdapter family allow-list");
        }
        if (applyingPaths.stream().noneMatch(path -> resolves(fieldResolverOf(path), name))) {
            return Optional.of("applied by " + applyingPaths + " but resolved by none of their FieldResolvers");
        }
        return Optional.empty();
    }

    /** The resolver each family's query adapters build their {@code FilterAdapter} with. */
    private static FieldResolver fieldResolverOf(AnalyticsSearchPath path) {
        return switch (path) {
            case HTTP, EDGE -> new HTTPFieldResolver();
            case MESSAGE -> new MessageFieldResolver();
            case NATIVE -> new NativeApiFieldResolver();
            case EVENT_METRICS -> new EventMetricsFieldResolver();
            case AUTHZ -> new AuthzFieldResolver();
            case AUTHZ_TRAFFIC -> new AuthzTrafficFieldResolver();
        };
    }

    private static boolean resolves(FieldResolver resolver, Filter.Name name) {
        try {
            return resolver.fromFilter(new Filter(name, Filter.Operator.EQ, "value")) != null;
        } catch (UnsupportedOperationException e) {
            return false;
        }
    }
}
