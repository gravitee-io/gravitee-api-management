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
package io.gravitee.apim.core.analytics_engine.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import fixtures.core.model.AuditInfoFixtures;
import io.gravitee.apim.core.analytics_engine.domain_service.AnalyticsQueryContextLoader;
import io.gravitee.apim.core.analytics_engine.domain_service.AnalyticsQueryValidator;
import io.gravitee.apim.core.analytics_engine.domain_service.BucketNamesPostProcessor;
import io.gravitee.apim.core.analytics_engine.domain_service.QueryFilterTransformer;
import io.gravitee.apim.core.analytics_engine.model.*;
import io.gravitee.apim.core.analytics_engine.query_service.AnalyticsEngineQueryService;
import io.gravitee.apim.core.analytics_engine.service_provider.AnalyticsQueryContextProvider;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ComputeTimeSeriesUseCaseTest {

    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo("org-id", "env-id", "user-id");

    private static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext("org-id", "env-id");

    private static final AnalyticsQueryContext ANALYTICS_CONTEXT = new AnalyticsQueryContext(
        AUDIT_INFO,
        EXECUTION_CONTEXT,
        Set.of("api-1", "api-2"),
        Map.of("api-1", "My API 1", "api-2", "My API 2"),
        Map.of(),
        Map.of()
    );

    @Mock
    private AnalyticsQueryContextProvider queryContextProvider;

    @Mock
    private AnalyticsQueryValidator validator;

    @Mock
    private AnalyticsQueryContextLoader contextLoader;

    @Mock
    private BucketNamesPostProcessor bucketNamesPostProcessor;

    @Mock
    private QueryFilterTransformer transformer1;

    @Mock
    private QueryFilterTransformer transformer2;

    @Mock
    private AnalyticsEngineQueryService queryService;

    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        when(contextLoader.load(any())).thenReturn(ANALYTICS_CONTEXT);
        when(bucketNamesPostProcessor.mapBucketNames(any(), any(), any(TimeSeriesResponse.class))).thenAnswer(inv -> inv.getArgument(2));
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    void should_load_context_from_audit_info() {
        var useCase = new ComputeTimeSeriesUseCase(queryContextProvider, validator, List.of(), bucketNamesPostProcessor, contextLoader);
        var request = aTimeSeriesRequest();

        when(queryContextProvider.resolve(request)).thenReturn(Map.of());

        useCase.execute(new ComputeTimeSeriesUseCase.Input(AUDIT_INFO, request));

        verify(contextLoader).load(AUDIT_INFO);
    }

    @Test
    void should_apply_transformer_filters_to_query() {
        var apiFilter = new Filter(FilterSpec.Name.API, FilterSpec.Operator.IN, Set.of("api-1"));
        when(transformer1.transform(eq(ANALYTICS_CONTEXT), any())).thenReturn(List.of(apiFilter));

        var useCase = new ComputeTimeSeriesUseCase(
            queryContextProvider,
            validator,
            List.of(transformer1),
            bucketNamesPostProcessor,
            contextLoader
        );
        var request = aTimeSeriesRequest();

        when(queryContextProvider.resolve(request)).thenReturn(Map.of(queryService, request));
        when(queryService.searchTimeSeries(any(), any())).thenReturn(new TimeSeriesResponse(List.of()));

        useCase.execute(new ComputeTimeSeriesUseCase.Input(AUDIT_INFO, request));

        var requestCaptor = ArgumentCaptor.forClass(TimeSeriesRequest.class);
        verify(queryService).searchTimeSeries(eq(EXECUTION_CONTEXT), requestCaptor.capture());

        assertThat(requestCaptor.getValue().filters()).contains(apiFilter);
    }

    @Test
    void should_chain_filters_from_multiple_transformers() {
        var filter1 = new Filter(FilterSpec.Name.API, FilterSpec.Operator.IN, Set.of("api-1"));
        var filter2 = new Filter(FilterSpec.Name.APPLICATION, FilterSpec.Operator.IN, Set.of("app-1"));

        when(transformer1.transform(eq(ANALYTICS_CONTEXT), any())).thenReturn(List.of(filter1));
        when(transformer2.transform(eq(ANALYTICS_CONTEXT), any())).thenReturn(List.of(filter1, filter2));

        var useCase = new ComputeTimeSeriesUseCase(
            queryContextProvider,
            validator,
            List.of(transformer1, transformer2),
            bucketNamesPostProcessor,
            contextLoader
        );
        var request = aTimeSeriesRequest();

        when(queryContextProvider.resolve(request)).thenReturn(Map.of(queryService, request));
        when(queryService.searchTimeSeries(any(), any())).thenReturn(new TimeSeriesResponse(List.of()));

        useCase.execute(new ComputeTimeSeriesUseCase.Input(AUDIT_INFO, request));

        var requestCaptor = ArgumentCaptor.forClass(TimeSeriesRequest.class);
        verify(queryService).searchTimeSeries(eq(EXECUTION_CONTEXT), requestCaptor.capture());

        assertThat(requestCaptor.getValue().filters()).containsExactly(filter1, filter2);
    }

    @Test
    void should_call_post_processor_with_analytics_context() {
        var useCase = new ComputeTimeSeriesUseCase(queryContextProvider, validator, List.of(), bucketNamesPostProcessor, contextLoader);
        var request = aTimeSeriesRequest();

        when(queryContextProvider.resolve(request)).thenReturn(Map.of(queryService, request));
        when(queryService.searchTimeSeries(any(), any())).thenReturn(new TimeSeriesResponse(List.of()));

        useCase.execute(new ComputeTimeSeriesUseCase.Input(AUDIT_INFO, request));

        verify(bucketNamesPostProcessor).mapBucketNames(eq(ANALYTICS_CONTEXT), eq(request.facets()), any(TimeSeriesResponse.class));
    }

    @Test
    void should_return_post_processed_response() {
        var rawResponse = new TimeSeriesResponse(List.of(new TimeSeriesMetricResponse(MetricSpec.Name.HTTP_REQUESTS, List.of())));

        var mappedResponse = new TimeSeriesResponse(List.of(new TimeSeriesMetricResponse(MetricSpec.Name.HTTP_REQUESTS, List.of())));

        when(bucketNamesPostProcessor.mapBucketNames(any(), any(), any(TimeSeriesResponse.class))).thenReturn(mappedResponse);

        var useCase = new ComputeTimeSeriesUseCase(queryContextProvider, validator, List.of(), bucketNamesPostProcessor, contextLoader);
        var request = aTimeSeriesRequest();

        when(queryContextProvider.resolve(request)).thenReturn(Map.of(queryService, request));
        when(queryService.searchTimeSeries(any(), any())).thenReturn(rawResponse);

        var output = useCase.execute(new ComputeTimeSeriesUseCase.Input(AUDIT_INFO, request));

        assertThat(output.response()).isEqualTo(mappedResponse);
    }

    @Test
    void should_pass_request_filters_to_transformer() {
        when(transformer1.transform(any(), any())).thenAnswer(inv -> inv.getArgument(1));

        var useCase = new ComputeTimeSeriesUseCase(
            queryContextProvider,
            validator,
            List.of(transformer1),
            bucketNamesPostProcessor,
            contextLoader
        );
        var request = aTimeSeriesRequest();

        when(queryContextProvider.resolve(request)).thenReturn(Map.of(queryService, request));
        when(queryService.searchTimeSeries(any(), any())).thenReturn(new TimeSeriesResponse(List.of()));

        useCase.execute(new ComputeTimeSeriesUseCase.Input(AUDIT_INFO, request));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Filter>> filtersCaptor = ArgumentCaptor.forClass(List.class);
        verify(transformer1).transform(eq(ANALYTICS_CONTEXT), filtersCaptor.capture());

        assertThat(filtersCaptor.getValue()).isEqualTo(request.filters());
    }

    private static TimeSeriesRequest aTimeSeriesRequest() {
        return new TimeSeriesRequest(
            new TimeRange(Instant.parse("2025-01-01T00:00:00Z"), Instant.parse("2025-01-02T00:00:00Z")),
            Duration.ofHours(1).toMillis(),
            List.of(),
            List.of(new FacetMetricMeasuresRequest(MetricSpec.Name.HTTP_REQUESTS, List.of(MetricSpec.Measure.COUNT), List.of())),
            List.of(),
            null,
            List.of()
        );
    }
}
