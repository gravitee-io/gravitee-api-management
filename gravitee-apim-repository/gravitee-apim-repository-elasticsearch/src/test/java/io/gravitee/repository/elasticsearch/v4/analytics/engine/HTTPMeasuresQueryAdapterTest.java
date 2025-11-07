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
package io.gravitee.repository.elasticsearch.v4.analytics.engine;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.repository.analytics.engine.Measure;
import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.Filter;
import io.gravitee.repository.analytics.engine.api.query.MeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.TimeRange;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter.HTTPMeasuresQueryAdapter;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
class HTTPMeasuresQueryAdapterTest {

    private static final String API_ID = "273f4728-1e30-4c78-bf47-281e304c78a5";
    private static final Long FROM = 1756104349879L;
    private static final Long TO = 1756190749879L;
    private static final ObjectMapper JSON = new ObjectMapper();

    final HTTPMeasuresQueryAdapter adapter = new HTTPMeasuresQueryAdapter();

    @Test
    void should_build_query() throws JsonProcessingException {
        var timeRange = new TimeRange(Instant.ofEpochMilli(FROM), Instant.ofEpochMilli(TO));
        var filters = List.of(new Filter(Filter.Name.API, Filter.Operator.IN, List.of(API_ID)));
        var measures = Set.of(Measure.P90);
        var metrics = List.of(
            new MetricMeasuresQuery(Metric.HTTP_GATEWAY_LATENCY, measures),
            new MetricMeasuresQuery(Metric.HTTP_GATEWAY_RESPONSE_TIME, measures)
        );

        var query = new MeasuresQuery(timeRange, filters, metrics);
        var queryString = adapter.adapt(query);
        var jsonQuery = JSON.readTree(queryString);

        var filter = jsonQuery.at("/query/bool/filter");
        System.out.println(jsonQuery);

        var from = filter.at("/0/range/@timestamp/gte");
        assertThat(from).isNotNull();
        assertThat(from.asLong()).isEqualTo(FROM);

        var to = filter.at("/0/range/@timestamp/lte");
        assertThat(to).isNotNull();
        assertThat(to.asLong()).isEqualTo(TO);

        var term = filter.at("/1/terms/api-id");
        assertThat(term).isNotNull();

        var termsValue = term.at("/0");
        assertThat(termsValue).isNotNull();
        assertThat(termsValue.asText()).isEqualTo(API_ID);

        var aggs = jsonQuery.at("/aggs");
        assertThat(aggs).isNotEmpty();

        var latencyP90 = aggs.at("/HTTP_GATEWAY_LATENCY#P90/percentiles");
        assertThat(latencyP90).isNotNull();

        var gatewayP90 = aggs.at("/HTTP_GATEWAY_RESPONSE_TIME#P90/percentiles");
        assertThat(gatewayP90).isNotNull();
    }
}
