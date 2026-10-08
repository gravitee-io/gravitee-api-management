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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.gamma.rest.core.observability.analytics.model.AnalyticsFacetMetricQuery;
import io.gravitee.gamma.rest.core.observability.analytics.port.service_provider.ObservabilityAnalyticsDataPort;
import io.gravitee.gamma.rest.core.observability.exception.InvalidObservabilityQueryException;
import io.gravitee.gamma.rest.core.observability.filter.domain_service.ObservabilityFilterValidator;
import io.gravitee.gamma.rest.core.observability.filter.port.service_provider.FilterRegistry;
import io.gravitee.gamma.rest.core.observability.logs.domain_service.AccessibleApiScopeDomainService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
            new AccessibleApiScopeDomainService()
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
            new AccessibleApiScopeDomainService()
        );
        var useCase = new ComputeObservabilityFacetsUseCase(analyticsDataPort, pipeline);
        when(analyticsDataPort.loadAccessibleApis("org-1", "env-1")).thenReturn(List.of());
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
}
