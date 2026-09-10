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

class HumanApprovalAnalyticsQueryServiceTest {

    private static final String PREFIX = "HUMAN_APPROVAL";

    private final AnalyticsRepository repository = mock(AnalyticsRepository.class);
    private final HumanApprovalAnalyticsQueryService service = new HumanApprovalAnalyticsQueryService(repository);

    @Test
    void should_declare_every_human_approval_metric() {
        assertThat(service.metrics()).containsExactlyInAnyOrder(MetricSpec.Name.HUMAN_APPROVALS, MetricSpec.Name.HUMAN_APPROVAL_COST);
    }

    @Test
    void should_delegate_measures_to_the_human_approval_repository_method() {
        var context = mock(ExecutionContext.class);
        when(repository.searchHumanApprovalMeasures(any(), any())).thenReturn(null);

        service.searchMeasures(context, null);

        verify(repository).searchHumanApprovalMeasures(any(), any());
    }

    @Test
    void should_delegate_facets_to_the_human_approval_repository_method() {
        var context = mock(ExecutionContext.class);
        when(repository.searchHumanApprovalFacets(any(), any())).thenReturn(null);

        service.searchFacets(context, null);

        verify(repository).searchHumanApprovalFacets(any(), any());
    }

    @Test
    void should_delegate_time_series_to_the_human_approval_repository_method() {
        var context = mock(ExecutionContext.class);
        when(repository.searchHumanApprovalTimeSeries(any(), any())).thenReturn(null);

        service.searchTimeSeries(context, null);

        verify(repository).searchHumanApprovalTimeSeries(any(), any());
    }

    @Test
    void should_route_every_human_approval_metric_the_enum_declares() {
        var unrouted = Arrays.stream(MetricSpec.Name.values())
            .filter(name -> name.name().startsWith(PREFIX))
            .filter(name -> !service.metrics().contains(name))
            .toList();

        assertThat(unrouted)
            .as("a HUMAN_APPROVAL metric absent here is declared but unroutable: the catalog offers it and no query service answers")
            .isEmpty();
    }

    @Test
    void should_not_claim_a_metric_from_another_domain() {
        var foreign = service
            .metrics()
            .stream()
            .filter(name -> !name.name().startsWith(PREFIX))
            .toList();

        assertThat(foreign).as("claiming a metric of another family would steal it from its own query service").isEmpty();
    }
}
