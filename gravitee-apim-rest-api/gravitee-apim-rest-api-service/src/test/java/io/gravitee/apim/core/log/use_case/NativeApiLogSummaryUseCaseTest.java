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
package io.gravitee.apim.core.log.use_case;

import static fixtures.core.model.NativeApiLogFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.analytics_engine.model.*;
import io.gravitee.apim.core.analytics_engine.query_service.AnalyticsEngineQueryService;
import io.gravitee.apim.core.analytics_engine.service_provider.AnalyticsQueryContextProvider;
import io.gravitee.apim.core.observability.model.FilterOperator;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Map;
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
class NativeApiLogSummaryUseCaseTest {

    private static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext("org-id", "env-id");

    @Mock
    private AnalyticsQueryContextProvider queryContextProvider;

    @Mock
    private AnalyticsEngineQueryService queryService;

    private NativeApiLogSummaryUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new NativeApiLogSummaryUseCase(queryContextProvider);
    }

    @Test
    void requests_native_connections_metric_grouped_by_connection_status() {
        when(queryContextProvider.resolve(any(FacetsRequest.class))).thenReturn(Map.of());

        useCase.execute(new NativeApiLogSummaryUseCase.Input(EXECUTION_CONTEXT, API_ID, FROM, TO));

        var captor = ArgumentCaptor.forClass(FacetsRequest.class);
        verify(queryContextProvider).resolve(captor.capture());

        var captured = captor.getValue();
        assertThat(captured.timeRange().from()).isEqualTo(FROM);
        assertThat(captured.timeRange().to()).isEqualTo(TO);
        assertThat(captured.filters()).containsExactly(new Filter(FilterSpec.Name.API, FilterOperator.EQ, API_ID));
        assertThat(captured.metrics()).hasSize(1);
        assertThat(captured.metrics().get(0).name()).isEqualTo(MetricSpec.Name.NATIVE_CONNECTIONS_SUMMARY);
        assertThat(captured.metrics().get(0).measures()).containsExactly(MetricSpec.Measure.COUNT);
        assertThat(captured.facets()).containsExactly(FacetSpec.Name.NATIVE_CONNECTION_STATUS);
    }

    @Test
    void flattens_buckets_into_count_by_connection_status() {
        var request = aFacetsRequest();
        when(queryContextProvider.resolve(any(FacetsRequest.class))).thenReturn(Map.of(queryService, request));
        when(queryService.searchFacets(eq(EXECUTION_CONTEXT), eq(request))).thenReturn(
            facetsResponseWithStatusCounts(CONNECTION_STATUS_COUNTS)
        );

        var output = useCase.execute(new NativeApiLogSummaryUseCase.Input(EXECUTION_CONTEXT, API_ID, FROM, TO));

        assertThat(output.countByConnectionStatus()).containsExactlyInAnyOrderEntriesOf(CONNECTION_STATUS_COUNTS);
    }

    @Test
    void returns_empty_map_when_no_query_service_resolved() {
        when(queryContextProvider.resolve(any(FacetsRequest.class))).thenReturn(Map.of());

        var output = useCase.execute(new NativeApiLogSummaryUseCase.Input(EXECUTION_CONTEXT, API_ID, FROM, TO));

        assertThat(output.countByConnectionStatus()).isEmpty();
    }

    @Test
    void ignores_buckets_without_count_measure() {
        var request = aFacetsRequest();
        when(queryContextProvider.resolve(any(FacetsRequest.class))).thenReturn(Map.of(queryService, request));
        when(queryService.searchFacets(any(), any())).thenReturn(
            new FacetsResponse(
                List.of(
                    new MetricFacetsResponse(
                        MetricSpec.Name.NATIVE_CONNECTIONS_SUMMARY,
                        null,
                        List.of(new FacetBucketResponse(STATUS_CONNECTED, STATUS_CONNECTED, List.of(), List.of()))
                    )
                )
            )
        );

        var output = useCase.execute(new NativeApiLogSummaryUseCase.Input(EXECUTION_CONTEXT, API_ID, FROM, TO));

        assertThat(output.countByConnectionStatus()).isEmpty();
    }

    private static FacetsRequest aFacetsRequest() {
        return new FacetsRequest(null, List.of(), List.of(), List.of(FacetSpec.Name.NATIVE_CONNECTION_STATUS), null, null);
    }

    private static FacetsResponse facetsResponseWithStatusCounts(Map<String, Long> countsByStatus) {
        var buckets = countsByStatus
            .entrySet()
            .stream()
            .map(entry ->
                new FacetBucketResponse(
                    entry.getKey(),
                    entry.getKey(),
                    List.of(),
                    List.of(new Measure(MetricSpec.Measure.COUNT, entry.getValue()))
                )
            )
            .toList();
        return new FacetsResponse(List.of(new MetricFacetsResponse(MetricSpec.Name.NATIVE_CONNECTIONS_SUMMARY, null, buckets)));
    }
}
