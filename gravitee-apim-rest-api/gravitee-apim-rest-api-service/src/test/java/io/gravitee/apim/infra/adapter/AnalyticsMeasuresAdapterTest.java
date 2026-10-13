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
package io.gravitee.apim.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.analytics_engine.model.Filter;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.MeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.TimeRange;
import io.gravitee.apim.core.analytics_engine.model.TrafficScope;
import io.gravitee.apim.core.observability.model.FilterOperator;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AnalyticsMeasuresAdapterTest {

    @Test
    void should_hand_the_engine_the_traffic_scope_in_its_own_model() {
        var request = new MeasuresRequest(
            new TimeRange(Instant.EPOCH, Instant.EPOCH.plusSeconds(60)),
            List.of(new Filter(FilterSpec.Name.TRAFFIC_SCOPE, FilterOperator.IN, new TrafficScope(Set.of("studio"), Set.of("mcp")))),
            List.of()
        );

        var query = AnalyticsMeasuresAdapter.INSTANCE.fromRequest(request);

        assertThat(query.filters()).containsExactly(
            new io.gravitee.repository.analytics.engine.api.query.Filter(
                io.gravitee.repository.analytics.engine.api.query.Filter.Name.TRAFFIC_SCOPE,
                io.gravitee.repository.analytics.engine.api.query.Filter.Operator.IN,
                new io.gravitee.repository.analytics.engine.api.query.TrafficScope(Set.of("studio"), Set.of("mcp"))
            )
        );
    }

    @Test
    void should_pass_any_other_filter_value_through() {
        var request = new MeasuresRequest(
            new TimeRange(Instant.EPOCH, Instant.EPOCH.plusSeconds(60)),
            List.of(new Filter(FilterSpec.Name.API, FilterOperator.IN, List.of("api-1"))),
            List.of()
        );

        var query = AnalyticsMeasuresAdapter.INSTANCE.fromRequest(request);

        assertThat(query.filters().getFirst().value()).isEqualTo(List.of("api-1"));
    }
}
