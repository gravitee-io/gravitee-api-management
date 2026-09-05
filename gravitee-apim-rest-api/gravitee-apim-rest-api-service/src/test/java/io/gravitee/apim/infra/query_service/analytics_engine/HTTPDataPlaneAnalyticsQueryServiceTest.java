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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.analytics_engine.model.Filter;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.GroupedMeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.Measure;
import io.gravitee.apim.core.analytics_engine.model.MetricMeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.MetricMeasuresResponse;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.analytics_engine.model.TimeRange;
import io.gravitee.apim.core.observability.model.FilterOperator;
import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.GroupedMeasuresQuery;
import io.gravitee.repository.analytics.engine.api.result.GroupedMeasuresResult;
import io.gravitee.repository.analytics.engine.api.result.MeasuresResult;
import io.gravitee.repository.analytics.engine.api.result.MetricMeasuresResult;
import io.gravitee.repository.log.v4.api.AnalyticsRepository;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class HTTPDataPlaneAnalyticsQueryServiceTest {

    private final AnalyticsRepository repository = mock(AnalyticsRepository.class);
    private final HTTPDataPlaneAnalyticsQueryService service = new HTTPDataPlaneAnalyticsQueryService(repository);

    @Test
    void should_compute_measures_per_group_through_the_repository() {
        var context = new ExecutionContext("org", "env");
        var groups = new LinkedHashMap<String, List<Filter>>();
        groups.put("a", List.of(new Filter(FilterSpec.Name.API, FilterOperator.IN, List.of("api-a"))));
        groups.put("b", List.of(new Filter(FilterSpec.Name.API, FilterOperator.IN, List.of("api-b"))));
        var request = new GroupedMeasuresRequest(
            new TimeRange(Instant.parse("2021-06-01T09:00:00Z"), Instant.parse("2021-06-01T10:00:00Z")),
            List.of(new Filter(FilterSpec.Name.API, FilterOperator.IN, List.of("api-a", "api-b"))),
            List.of(new MetricMeasuresRequest(MetricSpec.Name.HTTP_REQUESTS, List.of(MetricSpec.Measure.COUNT))),
            groups
        );
        when(repository.searchHTTPGroupedMeasures(eq(context.getQueryContext()), any())).thenReturn(
            new GroupedMeasuresResult(
                Map.of(
                    "a",
                    new MeasuresResult(
                        List.of(
                            new MetricMeasuresResult(
                                Metric.HTTP_REQUESTS,
                                Map.of(io.gravitee.repository.analytics.engine.api.metric.Measure.COUNT, 5)
                            )
                        )
                    )
                )
            )
        );

        var response = service.searchGroupedMeasures(context, request);

        var query = ArgumentCaptor.forClass(GroupedMeasuresQuery.class);
        verify(repository).searchHTTPGroupedMeasures(eq(context.getQueryContext()), query.capture());
        assertThat(query.getValue().timeRange().from()).isEqualTo(Instant.parse("2021-06-01T09:00:00Z"));
        assertThat(query.getValue().filters())
            .singleElement()
            .satisfies(filter -> {
                assertThat(filter.name()).isEqualTo(io.gravitee.repository.analytics.engine.api.query.Filter.Name.API);
                assertThat(filter.value()).isEqualTo(List.of("api-a", "api-b"));
            });
        assertThat(query.getValue().metrics())
            .singleElement()
            .satisfies(metric -> {
                assertThat(metric.metric()).isEqualTo(Metric.HTTP_REQUESTS);
                assertThat(metric.measures()).isEqualTo(Set.of(io.gravitee.repository.analytics.engine.api.metric.Measure.COUNT));
            });
        assertThat(query.getValue().groups().keySet()).containsExactly("a", "b");
        assertThat(query.getValue().groups().get("b"))
            .singleElement()
            .extracting(f -> f.value())
            .isEqualTo(List.of("api-b"));
        assertThat(response.groups().get("a").metrics()).containsExactly(
            new MetricMeasuresResponse(MetricSpec.Name.HTTP_REQUESTS, null, List.of(new Measure(MetricSpec.Measure.COUNT, 5)))
        );
    }

    @Test
    void should_advertise_grouped_measures() {
        assertThat(service.supportsGroupedMeasures()).isTrue();
    }
}
