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
package io.gravitee.apim.infra.domain_service.analytics_engine.definition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.apim.core.analytics_engine.model.ApiSpec;
import io.gravitee.apim.core.analytics_engine.model.FacetSpec;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.observability.model.FilterOperator;
import io.gravitee.apim.core.observability.model.FilterType;
import io.gravitee.apim.core.observability.model.Signal;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AnalyticsDefinitionYAMLQueryServiceTest {

    @Nested
    class NameIndex {

        @Test
        void should_resolve_a_filter_by_name() {
            var service = new AnalyticsDefinitionYAMLQueryService();

            assertThat(service.findFilter(FilterSpec.Name.HTTP_METHOD)).hasValueSatisfying(spec ->
                assertThat(spec.name()).isEqualTo(FilterSpec.Name.HTTP_METHOD)
            );
        }

        @Test
        void should_index_every_filter_the_catalog_declares() {
            var service = new AnalyticsDefinitionYAMLQueryService();

            assertThat(service.getAllFilters()).allSatisfy(spec -> assertThat(service.findFilter(spec.name())).contains(spec));
        }

        @Test
        void should_reject_a_catalog_declaring_the_same_filter_twice() {
            // A copy-pasted block would otherwise resolve to whichever entry loaded first, and the second one
            // would never be read — a silent catalog bug is exactly what the signal axis exists to prevent.
            var duplicated = List.of(filterNamed(FilterSpec.Name.HTTP_METHOD), filterNamed(FilterSpec.Name.HTTP_METHOD));

            assertThatThrownBy(() -> AnalyticsDefinitionYAMLQueryService.indexByName(duplicated))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HTTP_METHOD")
                .hasMessageContaining("declared more than once");
        }

        private FilterSpec filterNamed(FilterSpec.Name name) {
            return new FilterSpec(
                name,
                name.name(),
                FilterType.STRING,
                List.of(),
                null,
                List.of(FilterOperator.EQ),
                List.of(),
                Set.of(Signal.ANALYTICS)
            );
        }
    }

    @Nested
    class Apis {

        // getApis() is what GET /analytics/definition/apis returns, and a metric declaring an api kind
        // the list withholds leaves that kind's whole catalog unreachable.
        @Test
        void should_advertise_every_api_kind_a_metric_declares() {
            var service = new AnalyticsDefinitionYAMLQueryService();
            var advertised = service.getApis().stream().map(ApiSpec::name).toList();

            var declaredButUnadvertised = Arrays.stream(ApiSpec.Name.values())
                .filter(name -> !service.getMetrics(name).isEmpty())
                .filter(name -> !advertised.contains(name))
                .toList();

            assertThat(declaredButUnadvertised).isEmpty();
        }
    }

    @Nested
    class MetricDimensionParity {

        /**
         * Dimensions a metric lists that the top-level catalog does not declare, so {@code getFacets} /
         * {@code getFilters} drop them. All predate this contract, all sit on EDGE and MESSAGE metrics, and
         * whether each is an omission or a deliberate withholding is for the teams that own them — silently
         * cataloguing them here would surface pickers someone may have removed on purpose.
         *
         * <p>Pinned as an exact set rather than a floor: fixing one fails this test until it is struck off,
         * so the list can only shrink. Nothing may be added to it.
         */
        private static final List<String> KNOWN_UNCATALOGUED_FACETS = List.of(
            "EDGE_HEARTBEAT_COUNT -> EDGE_VERSION",
            "EDGE_TOKENS_IN -> EDGE_MODEL",
            "EDGE_TOKENS_IN -> EDGE_TOOL",
            "EDGE_TOKENS_OUT -> EDGE_MODEL",
            "EDGE_TOKENS_OUT -> EDGE_TOOL"
        );

        private static final List<String> KNOWN_UNCATALOGUED_FILTERS = List.of(
            "EDGE_HEARTBEAT_COUNT -> EDGE_VERSION",
            "EDGE_TOKENS_IN -> EDGE_MODEL",
            "EDGE_TOKENS_IN -> EDGE_TOOL",
            "EDGE_TOKENS_OUT -> EDGE_MODEL",
            "EDGE_TOKENS_OUT -> EDGE_TOOL",
            "MESSAGES -> MESSAGE_CONNECTOR_ID",
            "MESSAGE_ERRORS -> MESSAGE_CONNECTOR_ID",
            "MESSAGE_GATEWAY_LATENCY -> MESSAGE_CONNECTOR_ID",
            "MESSAGE_PAYLOAD_SIZE -> MESSAGE_CONNECTOR_ID"
        );

        /**
         * getFacets intersects the metric's own list with the top-level {@code facets:} catalog, so a name
         * listed on a metric but absent from the catalog is dropped there — the breakdown picker never offers
         * it, and nothing complains. Comparing the two through the public accessor measures that loss
         * directly rather than the state behind it.
         *
         * <p>The gap survives manual testing because it fails silently in both directions:
         * AnalyticsQueryValidator checks the metric's list rather than the catalog, so a hand-crafted request
         * still works while the UI never offers the dimension.
         */
        @Test
        void should_surface_every_facet_a_metric_lists() {
            assertThat(
                droppedDimensions(MetricSpec::facets, (service, name) -> service.getFacets(name).stream().map(FacetSpec::name).toList())
            )
                .as("facets listed on a metric but missing from the facets catalog are silently dropped by getFacets")
                .containsExactlyInAnyOrderElementsOf(KNOWN_UNCATALOGUED_FACETS);
        }

        /** Same contract on the filter side, which getFilters intersects the same way. */
        @Test
        void should_surface_every_filter_a_metric_lists() {
            assertThat(
                droppedDimensions(MetricSpec::filters, (service, name) -> service.getFilters(name).stream().map(FilterSpec::name).toList())
            ).containsExactlyInAnyOrderElementsOf(KNOWN_UNCATALOGUED_FILTERS);
        }

        private static <T> List<String> droppedDimensions(
            Function<MetricSpec, List<T>> declared,
            BiFunction<AnalyticsDefinitionYAMLQueryService, MetricSpec.Name, List<T>> surfaced
        ) {
            var service = new AnalyticsDefinitionYAMLQueryService();
            return Arrays.stream(ApiSpec.Name.values())
                .flatMap(api -> service.getMetrics(api).stream())
                .distinct()
                .flatMap(metric -> {
                    var visible = surfaced.apply(service, metric.name());
                    return declared
                        .apply(metric)
                        .stream()
                        .filter(dimension -> !visible.contains(dimension))
                        .map(dimension -> metric.name() + " -> " + dimension);
                })
                .distinct()
                .sorted()
                .toList();
        }
    }

    @Nested
    class AuthzFilters {

        /**
         * KEYWORD promises a value picker served from Elasticsearch, and no authz field has a mapping in
         * FilterValuesQueryServiceImpl, so a KEYWORD authz filter is a 500 the moment someone opens it.
         * Closed sets belong in ENUM, open ones in STRING.
         */
        @Test
        void should_not_type_any_authz_filter_as_keyword() {
            var service = new AnalyticsDefinitionYAMLQueryService();

            var keywordAuthzFilters = service
                .getAllFilters()
                .stream()
                .filter(spec -> spec.name().name().startsWith("AUTHZ_"))
                .filter(spec -> spec.type() == FilterType.KEYWORD)
                .map(FilterSpec::name)
                .toList();

            assertThat(keywordAuthzFilters).isEmpty();
        }
    }

    @Nested
    class AuthzFacets {

        // A search document carries no decision, so faceting searches on it yields one empty bucket.
        @Test
        void should_not_offer_decision_as_a_facet_of_searches() {
            var service = new AnalyticsDefinitionYAMLQueryService();

            var facetNames = service.getFacets(MetricSpec.Name.AUTHZ_SEARCHES).stream().map(FacetSpec::name).toList();

            assertThat(facetNames).doesNotContain(FacetSpec.Name.AUTHZ_DECISION);
        }

        // Same absent field: offered as a filter it would silently match nothing.
        @Test
        void should_not_offer_decision_as_a_filter_of_searches() {
            var service = new AnalyticsDefinitionYAMLQueryService();

            var filterNames = service.getFilters(MetricSpec.Name.AUTHZ_SEARCHES).stream().map(FilterSpec::name).toList();

            assertThat(filterNames).doesNotContain(FilterSpec.Name.AUTHZ_DECISION);
        }
    }
}
