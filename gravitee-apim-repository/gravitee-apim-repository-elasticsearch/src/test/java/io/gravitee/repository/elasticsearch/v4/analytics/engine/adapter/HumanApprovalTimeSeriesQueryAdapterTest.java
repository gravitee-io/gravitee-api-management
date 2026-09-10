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
package io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.analytics.engine.api.metric.Measure;
import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.TimeSeriesQuery;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class HumanApprovalTimeSeriesQueryAdapterTest extends AbstractQueryAdapterTest {

    private final HumanApprovalTimeSeriesQueryAdapter adapter = new HumanApprovalTimeSeriesQueryAdapter();

    private static final long ONE_MINUTE = 60_000L;

    private JsonObject adapt() {
        var query = new TimeSeriesQuery(
            buildTimeRange(),
            List.of(),
            ONE_MINUTE,
            List.of(new MetricMeasuresQuery(Metric.HUMAN_APPROVAL_COST, Set.of(Measure.COUNT))),
            List.of(),
            null,
            List.of()
        );
        return new JsonObject(adapter.adapt(query));
    }

    @Test
    void should_bucket_the_cost_over_time() {
        var series = adapt().getJsonObject("aggs").getJsonObject("HUMAN_APPROVAL_COST#TIME_SERIES");

        assertThat(series.getJsonObject("date_histogram").getString("fixed_interval")).isEqualTo("60000ms");
        assertThat(
            series.getJsonObject("aggs").getJsonObject("HUMAN_APPROVAL_COST#COUNT").getJsonObject("sum").getString("field")
        ).isEqualTo("additional-metrics.double_human-approval_cost");
    }

    @Test
    void should_carry_the_family_scope_into_a_time_series_query() {
        assertThat(adapt().getJsonObject("query").getJsonObject("bool").getJsonArray("filter").encode())
            .contains("human-approval")
            .contains("RESOLVED");
    }
}
