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
import io.gravitee.apim.core.analytics_engine.domain_service.QueryFilterTransformer;
import io.gravitee.apim.core.analytics_engine.model.*;
import io.gravitee.apim.core.analytics_engine.query_service.AnalyticsEngineQueryService;
import io.gravitee.apim.core.analytics_engine.service_provider.AnalyticsQueryContextProvider;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.rest.api.service.common.ExecutionContext;
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
class ComputeMeasuresUseCaseTest {

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
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    void should_load_context_from_audit_info() {
        var useCase = new ComputeMeasuresUseCase(queryContextProvider, validator, List.of(), contextLoader);
        var request = aMeasuresRequest();

        when(queryContextProvider.resolve(request)).thenReturn(Map.of());

        useCase.execute(new ComputeMeasuresUseCase.Input(AUDIT_INFO, request));

        verify(contextLoader).load(AUDIT_INFO);
    }

    @Test
    void should_apply_transformer_filters_to_query() {
        var apiFilter = new Filter(FilterSpec.Name.API, FilterSpec.Operator.IN, Set.of("api-1"));
        when(transformer1.transform(eq(ANALYTICS_CONTEXT), any())).thenReturn(List.of(apiFilter));

        var useCase = new ComputeMeasuresUseCase(queryContextProvider, validator, List.of(transformer1), contextLoader);
        var request = aMeasuresRequest();

        when(queryContextProvider.resolve(request)).thenReturn(Map.of(queryService, request));
        when(queryService.searchMeasures(any(), any())).thenReturn(new MeasuresResponse(List.of()));

        useCase.execute(new ComputeMeasuresUseCase.Input(AUDIT_INFO, request));

        var requestCaptor = ArgumentCaptor.forClass(MeasuresRequest.class);
        verify(queryService).searchMeasures(eq(EXECUTION_CONTEXT), requestCaptor.capture());

        assertThat(requestCaptor.getValue().filters()).contains(apiFilter);
    }

    @Test
    void should_chain_filters_from_multiple_transformers() {
        var filter1 = new Filter(FilterSpec.Name.API, FilterSpec.Operator.IN, Set.of("api-1"));
        var filter2 = new Filter(FilterSpec.Name.APPLICATION, FilterSpec.Operator.IN, Set.of("app-1"));

        when(transformer1.transform(eq(ANALYTICS_CONTEXT), any())).thenReturn(List.of(filter1));
        when(transformer2.transform(eq(ANALYTICS_CONTEXT), any())).thenReturn(List.of(filter1, filter2));

        var useCase = new ComputeMeasuresUseCase(queryContextProvider, validator, List.of(transformer1, transformer2), contextLoader);
        var request = aMeasuresRequest();

        when(queryContextProvider.resolve(request)).thenReturn(Map.of(queryService, request));
        when(queryService.searchMeasures(any(), any())).thenReturn(new MeasuresResponse(List.of()));

        useCase.execute(new ComputeMeasuresUseCase.Input(AUDIT_INFO, request));

        var requestCaptor = ArgumentCaptor.forClass(MeasuresRequest.class);
        verify(queryService).searchMeasures(eq(EXECUTION_CONTEXT), requestCaptor.capture());

        assertThat(requestCaptor.getValue().filters()).containsExactly(filter1, filter2);
    }

    @Test
    void should_pass_request_filters_to_transformer() {
        when(transformer1.transform(any(), any())).thenAnswer(inv -> inv.getArgument(1));

        var useCase = new ComputeMeasuresUseCase(queryContextProvider, validator, List.of(transformer1), contextLoader);
        var request = aMeasuresRequest();

        when(queryContextProvider.resolve(request)).thenReturn(Map.of(queryService, request));
        when(queryService.searchMeasures(any(), any())).thenReturn(new MeasuresResponse(List.of()));

        useCase.execute(new ComputeMeasuresUseCase.Input(AUDIT_INFO, request));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Filter>> filtersCaptor = ArgumentCaptor.forClass(List.class);
        verify(transformer1).transform(eq(ANALYTICS_CONTEXT), filtersCaptor.capture());

        assertThat(filtersCaptor.getValue()).isEqualTo(request.filters());
    }

    @Test
    void should_merge_responses_from_multiple_services() {
        var service1 = mock(AnalyticsEngineQueryService.class);
        var service2 = mock(AnalyticsEngineQueryService.class);

        var measure1 = new MetricMeasuresResponse(MetricSpec.Name.HTTP_REQUESTS, List.of(new Measure(MetricSpec.Measure.COUNT, 42)));
        var measure2 = new MetricMeasuresResponse(MetricSpec.Name.MESSAGES, List.of(new Measure(MetricSpec.Measure.COUNT, 10)));

        when(service1.searchMeasures(any(), any())).thenReturn(new MeasuresResponse(List.of(measure1)));
        when(service2.searchMeasures(any(), any())).thenReturn(new MeasuresResponse(List.of(measure2)));

        var useCase = new ComputeMeasuresUseCase(queryContextProvider, validator, List.of(), contextLoader);
        var request = aMeasuresRequest();

        var request1 = new MeasuresRequest(request.timeRange(), request.filters(), List.of(request.metrics().getFirst()));
        var request2 = new MeasuresRequest(
            request.timeRange(),
            request.filters(),
            List.of(new MetricMeasuresRequest(MetricSpec.Name.MESSAGES, List.of(MetricSpec.Measure.COUNT)))
        );

        when(queryContextProvider.resolve(request)).thenReturn(Map.of(service1, request1, service2, request2));

        var output = useCase.execute(new ComputeMeasuresUseCase.Input(AUDIT_INFO, request));

        assertThat(output.response().metrics()).containsExactlyInAnyOrder(measure1, measure2);
    }

    private static MeasuresRequest aMeasuresRequest() {
        return new MeasuresRequest(
            new TimeRange(Instant.parse("2025-01-01T00:00:00Z"), Instant.parse("2025-01-02T00:00:00Z")),
            List.of(),
            List.of(new MetricMeasuresRequest(MetricSpec.Name.HTTP_REQUESTS, List.of(MetricSpec.Measure.COUNT)))
        );
    }
}
