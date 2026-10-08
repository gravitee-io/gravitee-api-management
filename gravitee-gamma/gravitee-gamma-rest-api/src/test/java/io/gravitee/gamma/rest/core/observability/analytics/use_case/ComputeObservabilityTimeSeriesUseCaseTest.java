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

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.gravitee.gamma.rest.core.observability.analytics.model.AnalyticsFacetMetricQuery;
import io.gravitee.gamma.rest.core.observability.analytics.port.service_provider.ObservabilityAnalyticsDataPort;
import io.gravitee.gamma.rest.core.observability.exception.InvalidObservabilityQueryException;
import io.gravitee.gamma.rest.core.observability.filter.domain_service.ObservabilityFilterValidator;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.core.observability.filter.port.service_provider.FilterRegistry;
import io.gravitee.gamma.rest.core.observability.logs.domain_service.AccessibleApiScopeDomainService;
import io.gravitee.gamma.rest.core.observability.logs.port.service_provider.ObservabilityLogsDataPort.AccessibleApi;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ComputeObservabilityTimeSeriesUseCaseTest {

    private static final String ORG_ID = "org-1";
    private static final String ENV_ID = "env-1";
    private static final Instant FROM = Instant.parse("2026-06-10T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-06-11T00:00:00Z");
    private static final List<AnalyticsFacetMetricQuery> METRICS = List.of(
        new AnalyticsFacetMetricQuery("HTTP_REQUESTS", List.of("COUNT"), List.of(), List.of())
    );

    @Mock
    private ObservabilityAnalyticsDataPort analyticsDataPort;

    @Mock
    private FilterRegistry filterRegistry;

    private ComputeObservabilityTimeSeriesUseCase useCase;

    @BeforeEach
    void setUp() {
        var pipeline = new AnalyticsRequestPipeline(
            new ObservabilityFilterValidator(filterRegistry),
            new AccessibleApiScopeDomainService()
        );
        useCase = new ComputeObservabilityTimeSeriesUseCase(analyticsDataPort, pipeline);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = { 0L, -60_000L })
    void should_reject_a_missing_or_non_positive_interval_before_loading_the_accessible_apis(Long interval) {
        assertThatThrownBy(() -> useCase.execute(input(interval, METRICS)))
            .isInstanceOf(InvalidObservabilityQueryException.class)
            .extracting("technicalCode")
            .isEqualTo("observability.query.invalid_interval");
        verifyNoInteractions(analyticsDataPort);
    }

    @Test
    void should_reject_a_request_without_metrics_before_loading_the_accessible_apis() {
        assertThatThrownBy(() -> useCase.execute(input(60_000L, List.of())))
            .isInstanceOf(InvalidObservabilityQueryException.class)
            .extracting("technicalCode")
            .isEqualTo("observability.query.metrics_required");
        verifyNoInteractions(analyticsDataPort);
    }

    @Test
    void should_compute_a_well_formed_request() {
        when(analyticsDataPort.loadAccessibleApis(ORG_ID, ENV_ID)).thenReturn(
            List.of(new AccessibleApi("api-1", "API 1", ApiType.HTTP_PROXY))
        );
        var response = JsonNodeFactory.instance.objectNode();
        when(analyticsDataPort.computeTimeSeries(any())).thenReturn(response);

        var output = useCase.execute(input(60_000L, METRICS));

        assertThat(output.response()).isSameAs(response);
    }

    @Test
    void should_refuse_a_malformed_query_even_when_the_caller_can_read_no_api() {
        when(analyticsDataPort.loadAccessibleApis(ORG_ID, ENV_ID)).thenReturn(List.of());
        doThrow(InvalidObservabilityQueryException.unknownMeasure("HTTP_REQUESTS", "MEDIAN"))
            .when(analyticsDataPort)
            .validate(any(ObservabilityAnalyticsDataPort.TimeSeriesQuery.class));

        assertThatThrownBy(() -> useCase.execute(input(60_000L, METRICS))).isInstanceOf(InvalidObservabilityQueryException.class);
        verify(analyticsDataPort, never()).emptyTimeSeriesResponse();
    }

    private static ComputeObservabilityTimeSeriesUseCase.Input input(Long interval, List<AnalyticsFacetMetricQuery> metrics) {
        return new ComputeObservabilityTimeSeriesUseCase.Input(
            ORG_ID,
            ENV_ID,
            List.of(),
            FROM,
            TO,
            interval,
            metrics,
            List.of(),
            null,
            List.of()
        );
    }
}
