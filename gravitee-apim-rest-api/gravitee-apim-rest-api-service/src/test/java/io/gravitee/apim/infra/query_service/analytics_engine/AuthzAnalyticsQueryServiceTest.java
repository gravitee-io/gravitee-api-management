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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.repository.log.v4.api.AnalyticsRepository;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class AuthzAnalyticsQueryServiceTest {

    private final AnalyticsRepository repository = mock(AnalyticsRepository.class);
    private final AuthzAnalyticsQueryService service = new AuthzAnalyticsQueryService(repository);

    @Test
    void should_declare_every_authz_metric() {
        assertThat(service.metrics()).containsExactlyInAnyOrder(
            MetricSpec.Name.AUTHZ_OPERATIONS,
            MetricSpec.Name.AUTHZ_DECISIONS,
            MetricSpec.Name.AUTHZ_PERMITS,
            MetricSpec.Name.AUTHZ_FORBIDS,
            MetricSpec.Name.AUTHZ_NOT_APPLICABLE,
            MetricSpec.Name.AUTHZ_SEARCHES,
            MetricSpec.Name.AUTHZ_FAILURES,
            MetricSpec.Name.AUTHZ_EVAL_DURATION
        );
    }

    @Test
    void should_delegate_measures_to_the_authz_repository_method() {
        var context = mock(ExecutionContext.class);
        when(repository.searchAuthzMeasures(any(), any())).thenReturn(null);

        service.searchMeasures(context, null);

        verify(repository).searchAuthzMeasures(any(), any());
    }

    @Test
    void should_delegate_time_series_to_the_authz_repository_method() {
        var context = mock(ExecutionContext.class);
        when(repository.searchAuthzTimeSeries(any(), any())).thenReturn(null);

        service.searchTimeSeries(context, null);

        verify(repository).searchAuthzTimeSeries(any(), any());
    }

    @Test
    void should_delegate_facets_to_the_authz_repository_method() {
        var context = mock(ExecutionContext.class);
        when(repository.searchAuthzFacets(any(), any())).thenReturn(null);

        service.searchFacets(context, null);

        verify(repository).searchAuthzFacets(any(), any());
    }

    @Test
    void should_route_every_authz_metric_the_enum_declares() {
        var unrouted = Arrays.stream(MetricSpec.Name.values())
            .filter(name -> name.name().startsWith("AUTHZ_"))
            .filter(name -> !service.metrics().contains(name))
            .toList();

        assertThat(unrouted)
            .as("an AUTHZ_ metric absent here is declared but unroutable: the catalog offers it and no query service answers")
            .isEmpty();
    }

    @Test
    void should_not_claim_a_metric_from_another_domain() {
        var foreign = service
            .metrics()
            .stream()
            .filter(name -> !name.name().startsWith("AUTHZ_"))
            .toList();

        assertThat(foreign).as("claiming a non-authz metric would steal it from its own query service").isEmpty();
    }
}
