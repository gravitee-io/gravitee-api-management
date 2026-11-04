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

import static fixtures.core.model.NativeApiLogFixtures.*;
import static io.gravitee.repository.analytics.engine.api.metric.Measure.COUNT;
import static io.gravitee.repository.analytics.engine.api.query.Filter.Name.API;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.gravitee.apim.core.analytics_engine.model.*;
import io.gravitee.apim.core.observability.model.FilterOperator;
import io.gravitee.apim.infra.domain_service.analytics_engine.definition.AnalyticsDefinitionYAMLQueryService;
import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.Facet;
import io.gravitee.repository.analytics.engine.api.query.FacetsQuery;
import io.gravitee.repository.analytics.engine.api.query.MeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.TimeSeriesQuery;
import io.gravitee.repository.analytics.engine.api.result.FacetsResult;
import io.gravitee.repository.analytics.engine.api.result.MeasuresResult;
import io.gravitee.repository.analytics.engine.api.result.TimeSeriesResult;
import io.gravitee.repository.log.v4.api.AnalyticsRepository;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class NativeApiAnalyticsQueryServiceTest {

    private static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext("org-id", "env-id");

    @Mock
    private AnalyticsRepository analyticsRepository;

    private NativeApiAnalyticsQueryService service;

    @BeforeEach
    void setUp() {
        service = new NativeApiAnalyticsQueryService(analyticsRepository);
    }

    @Test
    void metrics_returns_native_connections_summary() {
        assertThat(service.metrics()).containsExactly(MetricSpec.Name.NATIVE_CONNECTIONS_SUMMARY);
    }

    /** The Elasticsearch native adapters build COUNT only: the catalog must serve no other measure for these metrics. */
    @Test
    void should_declare_count_as_the_only_measure_of_every_metric_this_service_searches() {
        var catalog = new AnalyticsDefinitionYAMLQueryService();

        for (var metric : service.metrics()) {
            assertThat(catalog.findMetric(metric))
                .as(metric.name())
                .hasValueSatisfying(spec -> assertThat(spec.measures()).containsExactly(MetricSpec.Measure.COUNT));
        }
    }

    @Test
    void searchFacets_forwards_api_filter_and_native_connection_status_facet() {
        when(analyticsRepository.searchNativeApiFacets(any(), any())).thenReturn(new FacetsResult(List.of()));
        var queryCaptor = ArgumentCaptor.forClass(FacetsQuery.class);

        var response = service.searchFacets(EXECUTION_CONTEXT, aFacetsRequest());

        assertThat(response.metrics()).isEmpty();
        verify(analyticsRepository).searchNativeApiFacets(eq(EXECUTION_CONTEXT.getQueryContext()), queryCaptor.capture());
        var captured = queryCaptor.getValue();
        assertThat(captured.facets()).contains(Facet.NATIVE_CONNECTION_STATUS);
        assertThat(captured.filters()).anySatisfy(f -> {
            assertThat(f.name()).isEqualTo(API);
            assertThat(f.value()).isEqualTo(API_ID);
        });
    }

    /** The definition serves NATIVE_CONNECTIONS_SUMMARY with COUNT: a KPI tile on it is answered, not refused. */
    @Test
    void searchMeasures_forwards_the_api_filter_and_the_connection_count_to_the_native_measures_search() {
        when(analyticsRepository.searchNativeApiMeasures(any(), any())).thenReturn(new MeasuresResult(List.of()));
        var queryCaptor = ArgumentCaptor.forClass(MeasuresQuery.class);

        var response = service.searchMeasures(EXECUTION_CONTEXT, aMeasuresRequest());

        assertThat(response.metrics()).isEmpty();
        verify(analyticsRepository).searchNativeApiMeasures(eq(EXECUTION_CONTEXT.getQueryContext()), queryCaptor.capture());
        var captured = queryCaptor.getValue();
        assertThat(captured.metrics())
            .singleElement()
            .satisfies(metric -> {
                assertThat(metric.metric()).isEqualTo(Metric.NATIVE_CONNECTIONS_SUMMARY);
                assertThat(metric.measures()).containsExactly(COUNT);
            });
        assertThat(captured.filters()).anySatisfy(f -> {
            assertThat(f.name()).isEqualTo(API);
            assertThat(f.value()).isEqualTo(API_ID);
        });
    }

    @Test
    void searchTimeSeries_forwards_api_filter_and_native_connection_status_facet() {
        when(analyticsRepository.searchNativeApiTimeSeries(any(), any())).thenReturn(new TimeSeriesResult(List.of()));
        var queryCaptor = ArgumentCaptor.forClass(TimeSeriesQuery.class);

        var response = service.searchTimeSeries(EXECUTION_CONTEXT, aTimeSeriesRequest());

        assertThat(response).isNotNull();
        verify(analyticsRepository).searchNativeApiTimeSeries(eq(EXECUTION_CONTEXT.getQueryContext()), queryCaptor.capture());
        var captured = queryCaptor.getValue();
        assertThat(captured.facets()).contains(Facet.NATIVE_CONNECTION_STATUS);
        assertThat(captured.interval()).isEqualTo(3_600_000L);
        assertThat(captured.filters()).anySatisfy(f -> {
            assertThat(f.name()).isEqualTo(API);
            assertThat(f.value()).isEqualTo(API_ID);
        });
    }

    private static FacetsRequest aFacetsRequest() {
        return new FacetsRequest(
            new TimeRange(FROM, TO),
            List.of(new Filter(FilterSpec.Name.API, FilterOperator.EQ, API_ID)),
            List.of(),
            List.of(FacetSpec.Name.NATIVE_CONNECTION_STATUS),
            null,
            null
        );
    }

    private static MeasuresRequest aMeasuresRequest() {
        return new MeasuresRequest(
            new TimeRange(FROM, TO),
            List.of(new Filter(FilterSpec.Name.API, FilterOperator.EQ, API_ID)),
            List.of(new MetricMeasuresRequest(MetricSpec.Name.NATIVE_CONNECTIONS_SUMMARY, List.of(MetricSpec.Measure.COUNT)))
        );
    }

    private static TimeSeriesRequest aTimeSeriesRequest() {
        return new TimeSeriesRequest(
            new TimeRange(FROM, TO),
            3_600_000L,
            List.of(new Filter(FilterSpec.Name.API, FilterOperator.EQ, API_ID)),
            List.of(
                new FacetMetricMeasuresRequest(
                    MetricSpec.Name.NATIVE_CONNECTIONS_SUMMARY,
                    List.of(MetricSpec.Measure.COUNT),
                    List.of(),
                    List.of()
                )
            ),
            List.of(FacetSpec.Name.NATIVE_CONNECTION_STATUS),
            null,
            List.of()
        );
    }
}
