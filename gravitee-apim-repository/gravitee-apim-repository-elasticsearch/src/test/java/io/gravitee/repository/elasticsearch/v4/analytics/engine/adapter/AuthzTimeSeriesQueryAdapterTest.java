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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.repository.analytics.engine.api.metric.Measure;
import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.Facet;
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.TimeSeriesQuery;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AuthzTimeSeriesQueryAdapterTest extends AbstractQueryAdapterTest {

    private final AuthzTimeSeriesQueryAdapter adapter = new AuthzTimeSeriesQueryAdapter();

    @Test
    void should_build_a_date_histogram_named_after_the_metric() {
        var query = new TimeSeriesQuery(
            buildTimeRange(),
            List.of(),
            86_400_000L,
            List.of(new MetricMeasuresQuery(Metric.AUTHZ_DECISIONS, Set.of(Measure.COUNT)))
        );

        var aggs = new JsonObject(adapter.adapt(query)).getJsonObject("aggs");
        var histogram = aggs.getJsonObject("AUTHZ_DECISIONS#TIME_SERIES");

        assertThat(aggs.fieldNames()).contains("AUTHZ_DECISIONS#TIME_SERIES");
        assertThat(histogram.containsKey("date_histogram")).isTrue();
        assertThat(histogram.getJsonObject("date_histogram").getString("field")).isEqualTo("@timestamp");
        assertThat(histogram.getJsonObject("date_histogram").containsKey("fixed_interval")).isTrue();
    }

    @Test
    void should_nest_a_facet_inside_the_date_histogram() {
        var query = new TimeSeriesQuery(
            buildTimeRange(),
            List.of(),
            86_400_000L,
            List.of(new MetricMeasuresQuery(Metric.AUTHZ_DECISIONS, Set.of(Measure.COUNT))),
            List.of(Facet.AUTHZ_DECISION),
            10,
            List.of()
        );

        var histogram = new JsonObject(adapter.adapt(query)).getJsonObject("aggs").getJsonObject("AUTHZ_DECISIONS#TIME_SERIES");
        var terms = histogram.getJsonObject("aggs").getJsonObject("AUTHZ_DECISIONS#AUTHZ_DECISION").getJsonObject("terms");

        assertThat(terms.getString("field")).isEqualTo("decision");
    }

    @Test
    void should_reject_more_than_one_facet() {
        var query = new TimeSeriesQuery(
            buildTimeRange(),
            List.of(),
            86_400_000L,
            List.of(new MetricMeasuresQuery(Metric.AUTHZ_DECISIONS, Set.of(Measure.COUNT))),
            List.of(Facet.AUTHZ_DECISION, Facet.AUTHZ_ACTION),
            10,
            List.of()
        );

        assertThatThrownBy(() -> adapter.adapt(query))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("single facet");
    }
}
