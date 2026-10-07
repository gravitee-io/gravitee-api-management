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
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.AuditInfoFixtures;
import io.gravitee.apim.core.analytics_engine.domain_service.AnalyticsQueryContextLoaderResolver;
import io.gravitee.apim.core.analytics_engine.domain_service.AnalyticsQueryValidator;
import io.gravitee.apim.core.analytics_engine.domain_service.UnitEnrichmentPostProcessor;
import io.gravitee.apim.core.analytics_engine.model.Filter;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.MeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.MeasuresResponse;
import io.gravitee.apim.core.analytics_engine.model.MetricMeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.analytics_engine.model.TimeRange;
import io.gravitee.apim.core.analytics_engine.query_service.AnalyticsEngineQueryService;
import io.gravitee.apim.core.analytics_engine.service_provider.AnalyticsQueryContextProvider;
import io.gravitee.apim.core.observability.model.FilterOperator;
import io.gravitee.apim.infra.domain_service.analytics_engine.processors.ApiTypeFilterTransformer;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ComputeMeasuresUseCaseAuthorizedApisTest {

    @Test
    void authorizes_only_the_given_apis_and_does_not_load_a_scope() {
        var loader = mock(AnalyticsQueryContextLoaderResolver.class);
        var enrichment = mock(UnitEnrichmentPostProcessor.class);
        when(enrichment.enrichUnits(any(MeasuresResponse.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var queryService = mock(AnalyticsEngineQueryService.class);
        when(queryService.metrics()).thenReturn(Set.of(MetricSpec.Name.HTTP_REQUESTS));
        when(queryService.searchMeasures(any(), any())).thenReturn(new MeasuresResponse(List.of()));
        var useCase = new ComputeMeasuresUseCase(
            new AnalyticsQueryContextProvider(List.of(queryService)),
            mock(AnalyticsQueryValidator.class),
            List.of(new ApiTypeFilterTransformer()),
            enrichment,
            loader
        );
        var request = new MeasuresRequest(
            new TimeRange(Instant.parse("2026-09-06T00:00:00Z"), Instant.parse("2026-10-06T00:00:00Z")),
            List.of(
                new Filter(FilterSpec.Name.API_PRODUCT, FilterOperator.EQ, "ws-1"),
                new Filter(FilterSpec.Name.APPLICATION, FilterOperator.EQ, "app-1")
            ),
            List.of(new MetricMeasuresRequest(MetricSpec.Name.HTTP_REQUESTS, List.of(MetricSpec.Measure.COUNT)))
        );

        useCase.executeForApis(AuditInfoFixtures.anAuditInfo("org-id", "env-id", "user-id"), request, Set.of("proxy-1"));

        verify(loader, never()).load(any(), any());
        var captured = ArgumentCaptor.forClass(MeasuresRequest.class);
        verify(queryService).searchMeasures(any(), captured.capture());
        assertThat(captured.getValue().filters())
            .extracting(Filter::name, Filter::value)
            .contains(
                tuple(FilterSpec.Name.API_PRODUCT, "ws-1"),
                tuple(FilterSpec.Name.APPLICATION, "app-1"),
                tuple(FilterSpec.Name.API, Set.of("proxy-1"))
            );
    }
}
