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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.analytics_engine.domain_service.AnalyticsQueryValidator;
import io.gravitee.apim.core.analytics_engine.model.FacetMetricMeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.FacetsResponse;
import io.gravitee.apim.core.analytics_engine.use_case.ComputeFacetsUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.ComputeMeasuresUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.ComputeTimeSeriesUseCase;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.user.domain_service.UserContextLoader;
import io.gravitee.apim.core.user.model.UserContext;
import io.gravitee.apim.infra.domain_service.analytics_engine.definition.AnalyticsDefinitionYAMLQueryService;
import io.gravitee.gamma.rest.core.observability.analytics.model.AnalyticsFacetMetricQuery;
import io.gravitee.gamma.rest.core.observability.analytics.model.AnalyticsMetricQuery;
import io.gravitee.gamma.rest.core.observability.analytics.model.AnalyticsSortSpec;
import io.gravitee.gamma.rest.core.observability.analytics.port.service_provider.ObservabilityAnalyticsDataPort.FacetsQuery;
import io.gravitee.gamma.rest.core.observability.analytics.port.service_provider.ObservabilityAnalyticsDataPort.MeasuresQuery;
import io.gravitee.gamma.rest.core.observability.analytics.port.service_provider.ObservabilityAnalyticsDataPort.TimeSeriesQuery;
import io.gravitee.gamma.rest.core.observability.analytics.use_case.AnalyticsRequestPipeline.PreparedScope;
import io.gravitee.gamma.rest.core.observability.exception.InvalidObservabilityQueryException;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterCondition;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterOperator;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ObservabilityAnalyticsDataPortAdapterTest {

    private static final String ORG = "org-1";
    private static final String ENV = "env-1";
    private static final PreparedScope SCOPE = new PreparedScope(
        Instant.parse("2026-06-10T00:00:00Z"),
        Instant.parse("2026-06-11T00:00:00Z"),
        List.of(new FilterCondition("API", FilterOperator.IN, List.of("api-1"))),
        Set.of("api-1")
    );

    @Mock
    private ComputeMeasuresUseCase computeMeasuresUseCase;

    @Mock
    private ComputeFacetsUseCase computeFacetsUseCase;

    @Mock
    private ComputeTimeSeriesUseCase computeTimeSeriesUseCase;

    @Mock
    private UserContextLoader userContextLoader;

    private ObservabilityAnalyticsDataPortAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ObservabilityAnalyticsDataPortAdapter(
            computeMeasuresUseCase,
            computeFacetsUseCase,
            computeTimeSeriesUseCase,
            new AnalyticsQueryValidator(new AnalyticsDefinitionYAMLQueryService()),
            userContextLoader,
            new ObjectMapper()
        );
    }

    @Test
    void should_map_an_authz_api_to_the_authz_gamma_api_type() {
        when(userContextLoader.loadApis(any())).thenAnswer(invocation ->
            ((UserContext) invocation.getArgument(0)).withApis(
                List.of(Api.builder().id("api-1").name("Authz API").type(io.gravitee.definition.model.v4.ApiType.AUTHZ).build())
            )
        );

        var accessibleApis = adapter.loadAccessibleApis(ORG, ENV);

        assertThat(accessibleApis).hasSize(1);
        assertThat(accessibleApis.getFirst().type()).isEqualTo(ApiType.AUTHZ);
    }

    @Nested
    class UnknownQueryNames {

        @Test
        void should_reject_an_unknown_metric_without_querying_the_engine() {
            var query = new MeasuresQuery(ORG, ENV, SCOPE, List.of(new AnalyticsMetricQuery("HTTP_NOPE", List.of("COUNT"))));

            assertThatThrownBy(() -> adapter.computeMeasures(query))
                .isInstanceOf(InvalidObservabilityQueryException.class)
                .hasMessageContaining("HTTP_NOPE")
                .extracting("technicalCode")
                .isEqualTo("observability.query.unknown_metric");
            verifyNoInteractions(computeMeasuresUseCase);
        }

        @Test
        void should_reject_a_metric_without_a_name() {
            var query = new MeasuresQuery(ORG, ENV, SCOPE, List.of(new AnalyticsMetricQuery(null, List.of("COUNT"))));

            assertThatThrownBy(() -> adapter.computeMeasures(query))
                .isInstanceOf(InvalidObservabilityQueryException.class)
                .extracting("technicalCode")
                .isEqualTo("observability.query.unknown_metric");
        }

        @Test
        void should_reject_an_unknown_measure() {
            var query = new MeasuresQuery(ORG, ENV, SCOPE, List.of(new AnalyticsMetricQuery("HTTP_REQUESTS", List.of("MEDIAN"))));

            assertThatThrownBy(() -> adapter.computeMeasures(query))
                .isInstanceOf(InvalidObservabilityQueryException.class)
                .hasMessageContaining("MEDIAN")
                .extracting("technicalCode")
                .isEqualTo("observability.query.unknown_measure");
        }

        @Test
        void should_reject_an_unknown_facet() {
            var query = facetsQuery(List.of("HTTP_NOPE"), List.of());

            assertThatThrownBy(() -> adapter.computeFacets(query))
                .isInstanceOf(InvalidObservabilityQueryException.class)
                .hasMessageContaining("HTTP_NOPE")
                .extracting("technicalCode")
                .isEqualTo("observability.query.unknown_facet");
            verifyNoInteractions(computeFacetsUseCase);
        }

        @Test
        void should_reject_an_unknown_sort_measure() {
            var query = facetsQuery(List.of("HTTP_STATUS"), List.of(new AnalyticsSortSpec("MEDIAN", "DESC")));

            assertThatThrownBy(() -> adapter.computeFacets(query))
                .isInstanceOf(InvalidObservabilityQueryException.class)
                .extracting("technicalCode")
                .isEqualTo("observability.query.unknown_measure");
        }

        @Test
        void should_reject_an_unknown_sort_order() {
            var query = facetsQuery(List.of("HTTP_STATUS"), List.of(new AnalyticsSortSpec("COUNT", "SIDEWAYS")));

            assertThatThrownBy(() -> adapter.computeFacets(query))
                .isInstanceOf(InvalidObservabilityQueryException.class)
                .hasMessageContaining("SIDEWAYS")
                .extracting("technicalCode")
                .isEqualTo("observability.query.unknown_sort_order");
        }

        @Test
        void should_read_the_sort_order_case_insensitively() {
            when(computeFacetsUseCase.execute(any())).thenReturn(new ComputeFacetsUseCase.Output(new FacetsResponse(List.of())));

            adapter.computeFacets(facetsQuery(List.of("HTTP_STATUS"), List.of(new AnalyticsSortSpec("COUNT", "desc"))));

            var captor = ArgumentCaptor.forClass(ComputeFacetsUseCase.Input.class);
            verify(computeFacetsUseCase).execute(captor.capture());
            assertThat(captor.getValue().request().metrics().getFirst().sorts().getFirst().order()).isEqualTo(
                FacetMetricMeasuresRequest.Sort.Order.DESC
            );
        }

        private static FacetsQuery facetsQuery(List<String> facets, List<AnalyticsSortSpec> sorts) {
            return new FacetsQuery(
                ORG,
                ENV,
                SCOPE,
                facets,
                null,
                List.of(new AnalyticsFacetMetricQuery("HTTP_REQUESTS", List.of("COUNT"), sorts)),
                List.of()
            );
        }
    }

    // Runs whatever the caller can read: the use cases call it before answering an empty scope.
    @Nested
    class Validate {

        private final PreparedScope emptyScope = PreparedScope.empty(SCOPE.from(), SCOPE.to());

        @Test
        void should_accept_a_well_formed_query_on_an_empty_scope() {
            var query = new MeasuresQuery(ORG, ENV, emptyScope, List.of(new AnalyticsMetricQuery("HTTP_REQUESTS", List.of("COUNT"))));

            adapter.validate(query);

            verifyNoInteractions(computeMeasuresUseCase);
        }

        @Test
        void should_refuse_an_unknown_metric_on_an_empty_scope() {
            var query = new MeasuresQuery(ORG, ENV, emptyScope, List.of(new AnalyticsMetricQuery("HTTP_NOPE", List.of("COUNT"))));

            assertThatThrownBy(() -> adapter.validate(query))
                .isInstanceOf(InvalidObservabilityQueryException.class)
                .extracting("technicalCode")
                .isEqualTo("observability.query.unknown_metric");
        }

        @Test
        void should_apply_the_engine_rules_on_an_empty_scope() {
            var query = new FacetsQuery(
                ORG,
                ENV,
                emptyScope,
                List.of(),
                null,
                List.of(new AnalyticsFacetMetricQuery("HTTP_REQUESTS", List.of("COUNT"), List.of())),
                List.of()
            );

            assertThatThrownBy(() -> adapter.validate(query))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("at least one facet");
        }

        @Test
        void should_refuse_an_unknown_measure_on_a_time_series_with_an_empty_scope() {
            var query = new TimeSeriesQuery(
                ORG,
                ENV,
                emptyScope,
                60_000L,
                List.of(),
                null,
                List.of(new AnalyticsFacetMetricQuery("HTTP_REQUESTS", List.of("MEDIAN"), List.of())),
                List.of()
            );

            assertThatThrownBy(() -> adapter.validate(query))
                .isInstanceOf(InvalidObservabilityQueryException.class)
                .extracting("technicalCode")
                .isEqualTo("observability.query.unknown_measure");
        }
    }
}
