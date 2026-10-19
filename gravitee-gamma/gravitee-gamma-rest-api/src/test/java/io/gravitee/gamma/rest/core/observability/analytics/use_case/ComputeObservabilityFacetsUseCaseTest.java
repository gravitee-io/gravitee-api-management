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
package io.gravitee.gamma.rest.core.observability.analytics.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.gamma.rest.core.observability.analytics.model.AnalyticsFacetMetricQuery;
import io.gravitee.gamma.rest.core.observability.analytics.model.DataKind;
import io.gravitee.gamma.rest.core.observability.analytics.port.service_provider.ObservabilityAnalyticsDataPort;
import io.gravitee.gamma.rest.core.observability.exception.InvalidObservabilityQueryException;
import io.gravitee.gamma.rest.core.observability.filter.domain_service.ObservabilityFilterValidator;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterCondition;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterOperator;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterSpec;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterType;
import io.gravitee.gamma.rest.core.observability.filter.model.Signal;
import io.gravitee.gamma.rest.core.observability.filter.port.service_provider.FilterRegistry;
import io.gravitee.gamma.rest.core.observability.logs.domain_service.AccessibleApiScopeDomainService;
import io.gravitee.gamma.rest.core.observability.logs.port.service_provider.ObservabilityLogsDataPort.AccessibleApi;
import io.gravitee.gamma.rest.infra.adapter.EntrypointScopeProviderAdapter;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ComputeObservabilityFacetsUseCaseTest {

    @Mock
    private ObservabilityAnalyticsDataPort analyticsDataPort;

    @Mock
    private FilterRegistry filterRegistry;

    @Test
    void should_reject_a_request_without_metrics_before_loading_the_accessible_apis() {
        var pipeline = new AnalyticsRequestPipeline(
            new ObservabilityFilterValidator(filterRegistry),
            new AccessibleApiScopeDomainService(),
            new EntrypointScopeProviderAdapter()
        );
        var useCase = new ComputeObservabilityFacetsUseCase(analyticsDataPort, pipeline);
        var input = new ComputeObservabilityFacetsUseCase.Input(
            "org-1",
            "env-1",
            List.of(),
            Instant.parse("2026-06-10T00:00:00Z"),
            Instant.parse("2026-06-11T00:00:00Z"),
            List.of(),
            List.of("HTTP_STATUS"),
            null,
            List.of()
        );

        assertThatThrownBy(() -> useCase.execute(input))
            .isInstanceOf(InvalidObservabilityQueryException.class)
            .extracting("technicalCode")
            .isEqualTo("observability.query.metrics_required");
        verifyNoInteractions(analyticsDataPort);
    }

    @Test
    void should_refuse_a_malformed_query_even_when_the_caller_can_read_no_api() {
        var pipeline = new AnalyticsRequestPipeline(
            new ObservabilityFilterValidator(filterRegistry),
            new AccessibleApiScopeDomainService(),
            new EntrypointScopeProviderAdapter()
        );
        var useCase = new ComputeObservabilityFacetsUseCase(analyticsDataPort, pipeline);
        when(analyticsDataPort.loadAccessibleApis("org-1", "env-1", DataKind.AGGREGATES)).thenReturn(List.of());
        doThrow(InvalidObservabilityQueryException.unknownFacet("BOGUS"))
            .when(analyticsDataPort)
            .validate(any(ObservabilityAnalyticsDataPort.FacetsQuery.class));
        var input = new ComputeObservabilityFacetsUseCase.Input(
            "org-1",
            "env-1",
            List.of(),
            Instant.parse("2026-06-10T00:00:00Z"),
            Instant.parse("2026-06-11T00:00:00Z"),
            List.of(new AnalyticsFacetMetricQuery("HTTP_REQUESTS", List.of("COUNT"), List.of(), List.of())),
            List.of("BOGUS"),
            null,
            List.of()
        );

        assertThatThrownBy(() -> useCase.execute(input)).isInstanceOf(InvalidObservabilityQueryException.class);
        verify(analyticsDataPort, never()).emptyFacetsResponse();
    }

    /** A breakdown by client IP is per-request data: it counts only the APIs whose requests the caller reads. */
    @Test
    void should_scope_a_breakdown_by_an_identifying_field_to_the_apis_whose_requests_the_caller_reads() {
        var pipeline = new AnalyticsRequestPipeline(
            new ObservabilityFilterValidator(filterRegistry),
            new AccessibleApiScopeDomainService(),
            new EntrypointScopeProviderAdapter()
        );
        var useCase = new ComputeObservabilityFacetsUseCase(analyticsDataPort, pipeline);
        when(analyticsDataPort.loadAccessibleApis("org-1", "env-1", DataKind.PER_REQUEST)).thenReturn(
            List.of(new AccessibleApi("logged-api", "Logged API", ApiType.HTTP_PROXY))
        );
        var input = new ComputeObservabilityFacetsUseCase.Input(
            "org-1",
            "env-1",
            List.of(),
            Instant.parse("2026-06-10T00:00:00Z"),
            Instant.parse("2026-06-11T00:00:00Z"),
            List.of(new AnalyticsFacetMetricQuery("HTTP_REQUESTS", List.of("COUNT"), List.of(), List.of())),
            List.of("CONSUMER_IP"),
            null,
            List.of()
        );

        useCase.execute(input);

        var captor = ArgumentCaptor.forClass(ObservabilityAnalyticsDataPort.FacetsQuery.class);
        verify(analyticsDataPort).computeFacets(captor.capture());
        assertThat(captor.getValue().scope().apiIds()).containsExactly("logged-api");
    }

    @Test
    void should_append_the_metric_conditions_every_metric_shares_to_the_query_filters() {
        givenTheStatusFilter();
        when(analyticsDataPort.loadAccessibleApis("org-1", "env-1", DataKind.AGGREGATES)).thenReturn(
            List.of(new AccessibleApi("api-1", "API 1", ApiType.HTTP_PROXY))
        );

        useCase().execute(input(List.of(STATUS_500), List.of(STATUS_500)));

        var captor = ArgumentCaptor.forClass(ObservabilityAnalyticsDataPort.FacetsQuery.class);
        verify(analyticsDataPort).computeFacets(captor.capture());
        assertThat(captor.getValue().scope().filters()).contains(STATUS_500);
        assertThat(captor.getValue().conditionsNotApplied()).isEmpty();
    }

    @Test
    void should_refuse_metrics_carrying_different_conditions_before_any_data_access() {
        givenTheStatusFilter();

        assertThatThrownBy(() -> useCase().execute(input(List.of(STATUS_500), List.of())))
            .isInstanceOf(InvalidObservabilityQueryException.class)
            .extracting("technicalCode")
            .isEqualTo("observability.query.differing_metric_conditions");
        verifyNoInteractions(analyticsDataPort);
    }

    private ComputeObservabilityFacetsUseCase useCase() {
        var pipeline = new AnalyticsRequestPipeline(
            new ObservabilityFilterValidator(filterRegistry),
            new AccessibleApiScopeDomainService(),
            new EntrypointScopeProviderAdapter()
        );
        return new ComputeObservabilityFacetsUseCase(analyticsDataPort, pipeline);
    }

    private static ComputeObservabilityFacetsUseCase.Input input(List<FilterCondition> first, List<FilterCondition> second) {
        return new ComputeObservabilityFacetsUseCase.Input(
            "org-1",
            "env-1",
            List.of(),
            Instant.parse("2026-06-10T00:00:00Z"),
            Instant.parse("2026-06-11T00:00:00Z"),
            List.of(
                new AnalyticsFacetMetricQuery("HTTP_REQUESTS", List.of("COUNT"), List.of(), first),
                new AnalyticsFacetMetricQuery("HTTP_ERRORS", List.of("COUNT"), List.of(), second)
            ),
            List.of("HTTP_STATUS"),
            null,
            List.of()
        );
    }

    private static final FilterCondition STATUS_500 = new FilterCondition("HTTP_STATUS", FilterOperator.EQ, List.of("500"));

    private void givenTheStatusFilter() {
        when(filterRegistry.getFilters(any(), any())).thenReturn(
            List.of(
                new FilterSpec(
                    "HTTP_STATUS",
                    "Status Code",
                    FilterType.NUMBER,
                    List.of(FilterOperator.EQ),
                    null,
                    new FilterSpec.Range(100, 599),
                    Set.of(Signal.ANALYTICS),
                    Set.of(ApiType.HTTP_PROXY)
                )
            )
        );
    }
}
