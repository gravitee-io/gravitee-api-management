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

import com.fasterxml.jackson.core.JsonProcessingException;
import io.gravitee.repository.analytics.engine.api.metric.Measure;
import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.Filter;
import io.gravitee.repository.analytics.engine.api.query.GroupedMeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class HTTPGroupedMeasuresQueryAdapterTest extends AbstractQueryAdapterTest {

    final HTTPGroupedMeasuresQueryAdapter adapter = new HTTPGroupedMeasuresQueryAdapter();

    @Test
    void should_build_one_filters_aggregation_with_a_named_bucket_per_group() throws JsonProcessingException {
        var groups = new LinkedHashMap<String, List<Filter>>();
        groups.put("alone", List.of(new Filter(Filter.Name.API, Filter.Operator.IN, List.of("api-a"))));
        groups.put(
            "pair-search",
            List.of(
                new Filter(Filter.Name.API, Filter.Operator.IN, List.of("api-b", "api-c")),
                new Filter(Filter.Name.MCP_PROXY_TOOL, Filter.Operator.EQ, "search")
            )
        );
        var metrics = List.of(
            new MetricMeasuresQuery(Metric.HTTP_REQUESTS, Set.of(Measure.COUNT)),
            new MetricMeasuresQuery(Metric.HTTP_GATEWAY_RESPONSE_TIME, Set.of(Measure.P95))
        );
        var query = new GroupedMeasuresQuery(
            buildTimeRange(),
            List.of(new Filter(Filter.Name.API, Filter.Operator.IN, List.of("api-a", "api-b", "api-c"))),
            metrics,
            groups
        );

        var jsonQuery = JSON.readTree(adapter.adapt(query));

        assertThat(jsonQuery.at("/size").asInt()).isZero();
        assertThat(jsonQuery.at("/query/bool/filter/0/range/@timestamp/gte").asLong()).isEqualTo(FROM);
        assertThat(jsonQuery.at("/query/bool/filter/1/terms/api-id")).hasSize(3);

        var filters = jsonQuery.at("/aggs/GROUPS/filters");
        assertThat(filters.at("/keyed").asBoolean()).isFalse();
        assertThat(filters.at("/filters/alone/bool/must/0/terms/api-id/0").asText()).isEqualTo("api-a");
        assertThat(filters.at("/filters/pair-search/bool/must/0/terms/api-id")).hasSize(2);
        assertThat(filters.at("/filters/pair-search/bool/must/1/term/additional-metrics.keyword_mcp-proxy_tools~1call").asText()).isEqualTo(
            "search"
        );

        var aggs = jsonQuery.at("/aggs/GROUPS/aggs");
        assertThat(aggs.at("/HTTP_REQUESTS#COUNT/value_count/field").asText()).isEqualTo("@timestamp");
        assertThat(aggs.at("/HTTP_GATEWAY_RESPONSE_TIME#P95/percentiles/percents/0").asDouble()).isEqualTo(95.0);
    }
}
