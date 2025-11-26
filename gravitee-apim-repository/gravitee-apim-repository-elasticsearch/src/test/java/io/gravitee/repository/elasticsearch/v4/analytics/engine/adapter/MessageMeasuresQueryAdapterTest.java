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

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.gravitee.repository.analytics.engine.api.metric.Measure;
import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.MeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
class MessageMeasuresQueryAdapterTest extends AbstractQueryAdapterTest {

    final MessageMeasuresQueryAdapter adapter = new MessageMeasuresQueryAdapter();

    private List<MetricMeasuresQuery> buildMessageMetrics() {
        var measures = Set.of(Measure.P90, Measure.P50, Measure.P95, Measure.P99, Measure.MIN, Measure.MAX, Measure.AVG);
        return List.of(
            new MetricMeasuresQuery(Metric.MESSAGE_GATEWAY_LATENCY, measures),
            new MetricMeasuresQuery(Metric.MESSAGES, Set.of(Measure.COUNT))
        );
    }

    @Test
    void should_build_query() throws JsonProcessingException {
        var timeRange = buildTimeRange();
        var filters = buildFilters();
        var metrics = buildMessageMetrics();
        var requestIDs = Set.of("request-id-1", "request-id-2", "request-id-3");

        var query = new MeasuresQuery(timeRange, filters, metrics);
        var queryString = adapter.adapt(query, requestIDs);
        var jsonQuery = JSON.readTree(queryString);

        assertThat(jsonQuery.at("/size").asInt()).isEqualTo(0);

        // Verify filter includes request IDs
        var filterArray = jsonQuery.at("/query/bool/filter");
        assertThat(filterArray).isNotNull();
        assertThat(filterArray.isArray()).isTrue();

        var requestIdFilter = filterArray.findValue("terms");
        assertThat(requestIdFilter).isNotNull();

        var requestIdTerms = requestIdFilter.at("/request-id");
        assertThat(requestIdTerms).isNotNull();
        assertThat(requestIdTerms.isArray()).isTrue();
        assertThat(requestIdTerms.size()).isEqualTo(3);

        // Verify aggregations
        var aggs = jsonQuery.at("/aggs");
        assertThat(aggs).isNotEmpty();

        var latencyP90 = aggs.at("/MESSAGE_GATEWAY_LATENCY#P90/percentiles/percents/0").asDouble();
        assertThat(latencyP90).isEqualTo(90.0);

        var latencyP95 = aggs.at("/MESSAGE_GATEWAY_LATENCY#P95/percentiles/percents/0").asDouble();
        assertThat(latencyP95).isEqualTo(95.0);

        var latencyP99 = aggs.at("/MESSAGE_GATEWAY_LATENCY#P99/percentiles/percents/0").asDouble();
        assertThat(latencyP99).isEqualTo(99.0);
    }

    @Test
    void should_build_query_without_request_ids() throws JsonProcessingException {
        var timeRange = buildTimeRange();
        var filters = buildFilters();
        var metrics = buildMessageMetrics();

        var query = new MeasuresQuery(timeRange, filters, metrics);
        var queryString = adapter.adapt(query, null);
        var jsonQuery = JSON.readTree(queryString);

        assertThat(jsonQuery.at("/size").asInt()).isEqualTo(0);

        // Verify no request ID filter when requestIDs is null
        var filterArray = jsonQuery.at("/query/bool/filter");
        assertThat(filterArray).isNotNull();
        assertThat(filterArray.isArray()).isTrue();

        var requestIdFilter = filterArray.findValue("terms");
        assertThat(requestIdFilter).isNull();
    }

    @Test
    void should_build_query_with_empty_request_ids() throws JsonProcessingException {
        var timeRange = buildTimeRange();
        var filters = buildFilters();
        var metrics = buildMessageMetrics();

        var query = new MeasuresQuery(timeRange, filters, metrics);
        var queryString = adapter.adapt(query, Set.of());
        var jsonQuery = JSON.readTree(queryString);

        assertThat(jsonQuery.at("/size").asInt()).isEqualTo(0);

        // Verify no request ID filter when requestIDs is empty
        var filterArray = jsonQuery.at("/query/bool/filter");
        assertThat(filterArray).isNotNull();
        assertThat(filterArray.isArray()).isTrue();

        var requestIdFilter = filterArray.findValue("terms");
        assertThat(requestIdFilter).isNull();
    }

    @Test
    void should_build_query_with_avg_measure() throws JsonProcessingException {
        var timeRange = buildTimeRange();
        var filters = buildFilters();
        var metrics = List.of(new MetricMeasuresQuery(Metric.MESSAGE_GATEWAY_LATENCY, Set.of(Measure.AVG)));
        var requestIDs = Set.of("request-id-1");

        var query = new MeasuresQuery(timeRange, filters, metrics);
        var queryString = adapter.adapt(query, requestIDs);
        var jsonQuery = JSON.readTree(queryString);

        var aggs = jsonQuery.at("/aggs");
        assertThat(aggs).isNotEmpty();

        var avgAgg = aggs.at("/MESSAGE_GATEWAY_LATENCY#AVG/avg/field");
        assertThat(avgAgg).isNotNull();
        assertThat(avgAgg.asText()).isEqualTo("gateway-latency-ms");
    }

    @Test
    void should_build_query_with_count_measure() throws JsonProcessingException {
        var timeRange = buildTimeRange();
        var filters = buildFilters();
        var metrics = List.of(new MetricMeasuresQuery(Metric.MESSAGES, Set.of(Measure.COUNT)));
        var requestIDs = Set.of("request-id-1");

        var query = new MeasuresQuery(timeRange, filters, metrics);
        var queryString = adapter.adapt(query, requestIDs);
        var jsonQuery = JSON.readTree(queryString);

        var aggs = jsonQuery.at("/aggs");
        assertThat(aggs).isNotEmpty();

        // COUNT uses sum aggregation for messages
        var countAgg = aggs.at("/MESSAGES#COUNT/sum/field");
        assertThat(countAgg).isNotNull();
        assertThat(countAgg.asText()).isEqualTo("count-increment");
    }

    @Test
    void should_build_query_with_max_measure() throws JsonProcessingException {
        var timeRange = buildTimeRange();
        var filters = buildFilters();
        var metrics = List.of(new MetricMeasuresQuery(Metric.MESSAGE_GATEWAY_LATENCY, Set.of(Measure.MAX)));
        var requestIDs = Set.of("request-id-1");

        var query = new MeasuresQuery(timeRange, filters, metrics);
        var queryString = adapter.adapt(query, requestIDs);
        var jsonQuery = JSON.readTree(queryString);

        var aggs = jsonQuery.at("/aggs");
        assertThat(aggs).isNotEmpty();

        var maxAgg = aggs.at("/MESSAGE_GATEWAY_LATENCY#MAX/max/field");
        assertThat(maxAgg).isNotNull();
        assertThat(maxAgg.asText()).isEqualTo("gateway-latency-ms");
    }

    @Test
    void should_build_query_with_min_measure() throws JsonProcessingException {
        var timeRange = buildTimeRange();
        var filters = buildFilters();
        var metrics = List.of(new MetricMeasuresQuery(Metric.MESSAGE_GATEWAY_LATENCY, Set.of(Measure.MIN)));
        var requestIDs = Set.of("request-id-1");

        var query = new MeasuresQuery(timeRange, filters, metrics);
        var queryString = adapter.adapt(query, requestIDs);
        var jsonQuery = JSON.readTree(queryString);

        var aggs = jsonQuery.at("/aggs");
        assertThat(aggs).isNotEmpty();

        var minAgg = aggs.at("/MESSAGE_GATEWAY_LATENCY#MIN/min/field");
        assertThat(minAgg).isNotNull();
        assertThat(minAgg.asText()).isEqualTo("gateway-latency-ms");
    }

    @Test
    void should_build_query_with_p50_measure() throws JsonProcessingException {
        var timeRange = buildTimeRange();
        var filters = buildFilters();
        var metrics = List.of(new MetricMeasuresQuery(Metric.MESSAGE_GATEWAY_LATENCY, Set.of(Measure.P50)));
        var requestIDs = Set.of("request-id-1");

        var query = new MeasuresQuery(timeRange, filters, metrics);
        var queryString = adapter.adapt(query, requestIDs);
        var jsonQuery = JSON.readTree(queryString);

        var aggs = jsonQuery.at("/aggs");
        assertThat(aggs).isNotEmpty();

        var p50Agg = aggs.at("/MESSAGE_GATEWAY_LATENCY#P50/percentiles/percents/0").asDouble();
        assertThat(p50Agg).isEqualTo(50.0);
    }
}
