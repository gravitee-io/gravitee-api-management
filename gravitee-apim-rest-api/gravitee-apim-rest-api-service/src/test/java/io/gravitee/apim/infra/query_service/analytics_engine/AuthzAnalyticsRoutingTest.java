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
import static org.mockito.Mockito.mock;

import io.gravitee.apim.core.analytics_engine.model.MeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.MetricMeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.analytics_engine.model.TimeRange;
import io.gravitee.apim.core.analytics_engine.service_provider.AnalyticsQueryContextProvider;
import io.gravitee.repository.log.v4.api.AnalyticsRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class AuthzAnalyticsRoutingTest {

    private final AnalyticsRepository repository = mock(AnalyticsRepository.class);
    private final AuthzAnalyticsQueryService decisions = new AuthzAnalyticsQueryService(repository);
    private final AuthzTrafficAnalyticsQueryService traffic = new AuthzTrafficAnalyticsQueryService(repository);
    private final AnalyticsQueryContextProvider provider = new AnalyticsQueryContextProvider(List.of(decisions, traffic));

    @Test
    void should_route_operations_and_searches_to_traffic_and_every_other_authz_metric_to_decisions() {
        assertThat(provider.resolve(MetricSpec.Name.AUTHZ_OPERATIONS)).isSameAs(traffic);
        assertThat(provider.resolve(MetricSpec.Name.AUTHZ_SEARCHES)).isSameAs(traffic);
        Arrays.stream(MetricSpec.Name.values())
            .filter(name -> name.name().startsWith("AUTHZ_"))
            .filter(name -> name != MetricSpec.Name.AUTHZ_OPERATIONS && name != MetricSpec.Name.AUTHZ_SEARCHES)
            .forEach(name -> assertThat(provider.resolve(name)).as(name.name()).isSameAs(decisions));
    }

    @Test
    void should_not_let_two_authz_services_claim_the_same_metric() {
        var claimedByBoth = new HashSet<>(decisions.metrics());
        claimedByBoth.retainAll(traffic.metrics());

        assertThat(claimedByBoth).isEmpty();
    }

    @Test
    void should_split_a_kpi_request_mixing_decisions_and_searches_between_both_services() {
        var request = new MeasuresRequest(
            new TimeRange(Instant.parse("2025-01-01T00:00:00Z"), Instant.parse("2025-01-02T00:00:00Z")),
            new ArrayList<>(),
            new ArrayList<>(
                List.of(
                    new MetricMeasuresRequest(MetricSpec.Name.AUTHZ_DECISIONS, List.of(MetricSpec.Measure.COUNT)),
                    new MetricMeasuresRequest(MetricSpec.Name.AUTHZ_SEARCHES, List.of(MetricSpec.Measure.COUNT))
                )
            )
        );

        var context = provider.resolve(request);

        assertThat(context).containsOnlyKeys(decisions, traffic);
        assertThat(context.get(decisions).metrics())
            .extracting(MetricMeasuresRequest::name)
            .containsExactly(MetricSpec.Name.AUTHZ_DECISIONS);
        assertThat(context.get(traffic).metrics()).extracting(MetricMeasuresRequest::name).containsExactly(MetricSpec.Name.AUTHZ_SEARCHES);
    }
}
