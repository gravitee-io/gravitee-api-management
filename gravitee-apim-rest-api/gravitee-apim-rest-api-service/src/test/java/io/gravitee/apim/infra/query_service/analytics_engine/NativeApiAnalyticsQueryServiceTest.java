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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.gravitee.apim.core.analytics_engine.model.*;
import io.gravitee.apim.core.observability.model.FilterOperator;
import io.gravitee.repository.analytics.engine.api.query.Facet;
import io.gravitee.repository.analytics.engine.api.query.FacetsQuery;
import io.gravitee.repository.analytics.engine.api.result.FacetsResult;
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
            assertThat(f.name()).isEqualTo(io.gravitee.repository.analytics.engine.api.query.Filter.Name.API);
            assertThat(f.value()).isEqualTo(API_ID);
        });
    }

    @Test
    void searchMeasures_throws_UnsupportedOperationException() {
        assertThatThrownBy(() -> service.searchMeasures(EXECUTION_CONTEXT, aMeasuresRequest()))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("measures");
        verifyNoInteractions(analyticsRepository);
    }

    @Test
    void searchTimeSeries_throws_UnsupportedOperationException() {
        assertThatThrownBy(() -> service.searchTimeSeries(EXECUTION_CONTEXT, aTimeSeriesRequest()))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("time series");
        verifyNoInteractions(analyticsRepository);
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
        return new MeasuresRequest(new TimeRange(FROM, TO), List.of(), List.of());
    }

    private static TimeSeriesRequest aTimeSeriesRequest() {
        return new TimeSeriesRequest(new TimeRange(FROM, TO), null, List.of());
    }
}
