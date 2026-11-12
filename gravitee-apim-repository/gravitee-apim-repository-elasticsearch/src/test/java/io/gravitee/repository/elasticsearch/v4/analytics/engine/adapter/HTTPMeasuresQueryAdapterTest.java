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
import io.gravitee.repository.analytics.engine.api.query.Filter;
import io.gravitee.repository.analytics.engine.api.query.MeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
class HTTPMeasuresQueryAdapterTest extends AbstractQueryAdapterTest {

    final HTTPMeasuresQueryAdapter adapter = new HTTPMeasuresQueryAdapter();

    @Test
    void should_build_query() throws JsonProcessingException {
        var timeRange = buildTimeRange();
        var filters = buildFilters();
        var metrics = buildMetrics();

        var query = new MeasuresQuery(timeRange, filters, metrics);

        var queryString = adapter.adapt(query);

        var jsonQuery = JSON.readTree(queryString);

        var filter = jsonQuery.at("/query/bool/filter");

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

        var latencyP90 = aggs.at("/HTTP_GATEWAY_LATENCY#__MEASURED__/aggs/HTTP_GATEWAY_LATENCY#P90/percentiles/percents/0").asDouble();
        assertThat(latencyP90).isEqualTo(90.0);

        var latencyP95 = aggs.at("/HTTP_GATEWAY_LATENCY#__MEASURED__/aggs/HTTP_GATEWAY_LATENCY#P95/percentiles/percents/0").asDouble();
        assertThat(latencyP95).isEqualTo(95.0);

        var latencyP99 = aggs.at("/HTTP_GATEWAY_LATENCY#__MEASURED__/aggs/HTTP_GATEWAY_LATENCY#P99/percentiles/percents/0").asDouble();
        assertThat(latencyP99).isEqualTo(99.0);

        var gatewayP90 = aggs
            .at("/HTTP_GATEWAY_RESPONSE_TIME#__MEASURED__/aggs/HTTP_GATEWAY_RESPONSE_TIME#P90/percentiles/percents/0")
            .asDouble();
        assertThat(gatewayP90).isEqualTo(90.0);
    }

    @Test
    void should_build_query_with_avg_measure() throws JsonProcessingException {
        var timeRange = buildTimeRange();
        var filters = buildFilters();
        var metrics = List.of(new MetricMeasuresQuery(Metric.HTTP_GATEWAY_LATENCY, Set.of(Measure.AVG)));

        var query = new MeasuresQuery(timeRange, filters, metrics);

        var queryString = adapter.adapt(query);

        var jsonQuery = JSON.readTree(queryString);

        var aggs = jsonQuery.at("/aggs");
        assertThat(aggs).isNotEmpty();

        var avgAgg = aggs.at("/HTTP_GATEWAY_LATENCY#__MEASURED__/aggs/HTTP_GATEWAY_LATENCY#AVG/avg/field");
        assertThat(avgAgg).isNotNull();
        assertThat(avgAgg.asText()).isEqualTo("gateway-latency-ms");
        assertThat(aggs.at("/HTTP_GATEWAY_LATENCY#__MEASURED__/filter/bool/must_not/0/term/connection-phase").asText()).isEqualTo("open");
    }

    @Test
    void should_build_query_with_count_measure() throws JsonProcessingException {
        var timeRange = buildTimeRange();
        var filters = buildFilters();
        var metrics = List.of(new MetricMeasuresQuery(Metric.HTTP_REQUESTS, Set.of(Measure.COUNT)));

        var query = new MeasuresQuery(timeRange, filters, metrics);

        var queryString = adapter.adapt(query);

        var jsonQuery = JSON.readTree(queryString);

        var aggs = jsonQuery.at("/aggs");
        assertThat(aggs).isNotEmpty();

        var countAgg = aggs.at("/HTTP_REQUESTS#__COUNTED__/aggs/HTTP_REQUESTS#COUNT/value_count/field");
        assertThat(countAgg).isNotNull();
        assertThat(countAgg.asText()).isEqualTo("@timestamp");
    }

    @Test
    void should_build_query_with_max_measure() throws JsonProcessingException {
        var timeRange = buildTimeRange();
        var filters = buildFilters();
        var metrics = List.of(new MetricMeasuresQuery(Metric.HTTP_GATEWAY_LATENCY, Set.of(Measure.MAX)));

        var query = new MeasuresQuery(timeRange, filters, metrics);

        var queryString = adapter.adapt(query);

        var jsonQuery = JSON.readTree(queryString);

        var aggs = jsonQuery.at("/aggs");
        assertThat(aggs).isNotEmpty();

        var maxAgg = aggs.at("/HTTP_GATEWAY_LATENCY#__MEASURED__/aggs/HTTP_GATEWAY_LATENCY#MAX/max/field");
        assertThat(maxAgg).isNotNull();
        assertThat(maxAgg.asText()).isEqualTo("gateway-latency-ms");
    }

    @Test
    void should_build_query_with_min_measure() throws JsonProcessingException {
        var timeRange = buildTimeRange();
        var filters = buildFilters();
        var metrics = List.of(new MetricMeasuresQuery(Metric.HTTP_GATEWAY_LATENCY, Set.of(Measure.MIN)));

        var query = new MeasuresQuery(timeRange, filters, metrics);

        var queryString = adapter.adapt(query);

        var jsonQuery = JSON.readTree(queryString);

        var aggs = jsonQuery.at("/aggs");
        assertThat(aggs).isNotEmpty();

        var minAgg = aggs.at("/HTTP_GATEWAY_LATENCY#__MEASURED__/aggs/HTTP_GATEWAY_LATENCY#MIN/min/field");
        assertThat(minAgg).isNotNull();
        assertThat(minAgg.asText()).isEqualTo("gateway-latency-ms");
    }

    @Test
    void should_build_query_with_p50_measure() throws JsonProcessingException {
        var timeRange = buildTimeRange();
        var filters = buildFilters();
        var metrics = List.of(new MetricMeasuresQuery(Metric.HTTP_GATEWAY_LATENCY, Set.of(Measure.P50)));

        var query = new MeasuresQuery(timeRange, filters, metrics);

        var queryString = adapter.adapt(query);

        var jsonQuery = JSON.readTree(queryString);

        var aggs = jsonQuery.at("/aggs");
        assertThat(aggs).isNotEmpty();

        var p50Agg = aggs.at("/HTTP_GATEWAY_LATENCY#__MEASURED__/aggs/HTTP_GATEWAY_LATENCY#P50/percentiles/percents/0").asDouble();
        assertThat(p50Agg).isEqualTo(50.0);
    }

    @Test
    void should_build_query_with_error_rate_metric() throws JsonProcessingException {
        var timeRange = buildTimeRange();
        var filters = buildFilters();
        var metrics = List.of(new MetricMeasuresQuery(Metric.HTTP_ERROR_RATE, Set.of(Measure.PERCENTAGE)));

        var query = new MeasuresQuery(timeRange, filters, metrics);

        var queryString = adapter.adapt(query);

        var jsonQuery = JSON.readTree(queryString);

        var aggs = jsonQuery.at("/aggs");
        assertThat(aggs).isNotEmpty();

        var errorRateDateHistogram = aggs.at("/_HTTP_ERROR_RATE#PERCENTAGE/date_histogram");
        assertThat(errorRateDateHistogram).isNotNull();

        var errorRateBucketScript = aggs.at("/_HTTP_ERROR_RATE#PERCENTAGE/aggs/HTTP_ERROR_RATE#PERCENTAGE/bucket_script");
        assertThat(errorRateBucketScript).isNotNull();
        assertThat(errorRateBucketScript.has("script")).isTrue();
    }

    @ParameterizedTest
    @CsvSource(
        {
            "LLM_PROMPT_TOKEN_SENT, additional-metrics.long_llm-proxy_tokens-sent",
            "LLM_PROMPT_TOKEN_RECEIVED, additional-metrics.long_llm-proxy_tokens-received",
            "LLM_PROMPT_TOKEN_SENT_COST, additional-metrics.double_llm-proxy_sent-cost",
            "LLM_PROMPT_TOKEN_RECEIVED_COST, additional-metrics.double_llm-proxy_received-cost",
        }
    )
    void should_count_the_requests_carrying_an_llm_value_and_sum_it(Metric metric, String field) throws JsonProcessingException {
        var metrics = List.of(new MetricMeasuresQuery(metric, Set.of(Measure.COUNT, Measure.SUM)));

        var aggs = JSON.readTree(adapter.adapt(new MeasuresQuery(buildTimeRange(), buildFilters(), metrics))).at("/aggs");

        assertThat(aggs.at("/" + metric + "#__COUNTED__/aggs/" + metric + "#COUNT")).isEqualTo(
            JSON.readTree("{\"value_count\":{\"field\":\"" + field + "\"}}")
        );
        assertThat(aggs.at("/" + metric + "#__MEASURED__/aggs/" + metric + "#SUM")).isEqualTo(
            JSON.readTree("{\"sum\":{\"field\":\"" + field + "\"}}")
        );
    }

    @ParameterizedTest
    @CsvSource(
        {
            "LLM_PROMPT_TOTAL_TOKEN, COUNT, value_count, long_llm-proxy_tokens-sent, long_llm-proxy_tokens-received",
            "LLM_PROMPT_TOTAL_TOKEN, SUM, sum, long_llm-proxy_tokens-sent, long_llm-proxy_tokens-received",
            "LLM_PROMPT_TOTAL_TOKEN, AVG, avg, long_llm-proxy_tokens-sent, long_llm-proxy_tokens-received",
            "LLM_PROMPT_TOKEN_TOTAL_COST, COUNT, value_count, double_llm-proxy_sent-cost, double_llm-proxy_received-cost",
            "LLM_PROMPT_TOKEN_TOTAL_COST, SUM, sum, double_llm-proxy_sent-cost, double_llm-proxy_received-cost",
            "LLM_PROMPT_TOKEN_TOTAL_COST, AVG, avg, double_llm-proxy_sent-cost, double_llm-proxy_received-cost",
        }
    )
    void should_measure_an_llm_total_over_both_of_its_values(Metric metric, Measure measure, String type, String sent, String received)
        throws JsonProcessingException {
        var metrics = List.of(new MetricMeasuresQuery(metric, Set.of(measure)));

        var aggs = JSON.readTree(adapter.adapt(new MeasuresQuery(buildTimeRange(), buildFilters(), metrics))).at("/aggs");

        var envelope = measure == Measure.COUNT ? "#__COUNTED__" : "#__MEASURED__";
        var source = aggs.at("/" + metric + envelope + "/aggs/" + metric + "#" + measure + "/" + type + "/script/source").asText();
        assertThat(source).contains("additional-metrics." + sent).contains("additional-metrics." + received);
    }

    @Test
    void should_add_the_metric_filters_to_the_envelope_of_its_counts() throws JsonProcessingException {
        var timeRange = buildTimeRange();
        var filters = buildFilters();
        var metricFilters = List.of(new Filter(Filter.Name.API, Filter.Operator.IN, List.of("id1", "id2")));
        var metrics = List.of(new MetricMeasuresQuery(Metric.HTTP_REQUESTS, Set.of(Measure.COUNT), metricFilters, List.of()));

        var query = new MeasuresQuery(timeRange, filters, metrics);

        var queryString = adapter.adapt(query);

        var jsonQuery = JSON.readTree(queryString);

        var aggs = jsonQuery.at("/aggs");
        assertThat(aggs).isNotEmpty();

        var filterAgg = aggs.at("/HTTP_REQUESTS#__COUNTED__");
        assertThat(filterAgg.isMissingNode()).isFalse();
        assertThat(filterAgg.at("/filter/bool/must_not/0/term/connection-phase").asText()).isEqualTo("end");

        var filterClause = filterAgg.at("/filter/bool/filter/0/bool/must/0/terms/api-id");
        assertThat(filterClause.isMissingNode()).isFalse();
        assertThat(filterClause.get(0).asText()).isEqualTo("id1");
        assertThat(filterClause.get(1).asText()).isEqualTo("id2");

        var nestedCountAgg = filterAgg.at("/aggs/HTTP_REQUESTS#COUNT/value_count/field");
        assertThat(nestedCountAgg.isMissingNode()).isFalse();
        assertThat(nestedCountAgg.asText()).isEqualTo("@timestamp");
    }

    @Test
    void should_filter_the_counts_of_a_metric_without_filters_on_the_connection_phase_only() throws JsonProcessingException {
        var timeRange = buildTimeRange();
        var filters = buildFilters();
        var metrics = List.of(new MetricMeasuresQuery(Metric.HTTP_REQUESTS, Set.of(Measure.COUNT)));

        var query = new MeasuresQuery(timeRange, filters, metrics);

        var queryString = adapter.adapt(query);

        var jsonQuery = JSON.readTree(queryString);

        var aggs = jsonQuery.at("/aggs");

        assertThat(aggs.at("/HTTP_REQUESTS#__FILTER__").isMissingNode()).isTrue();
        assertThat(aggs.at("/HTTP_REQUESTS#__COUNTED__/filter/bool/filter").isMissingNode()).isTrue();
        assertThat(aggs.at("/HTTP_REQUESTS#__COUNTED__/filter/bool/must_not/0/term/connection-phase").asText()).isEqualTo("end");

        var countAgg = aggs.at("/HTTP_REQUESTS#__COUNTED__/aggs/HTTP_REQUESTS#COUNT/value_count/field");
        assertThat(countAgg.isMissingNode()).isFalse();
        assertThat(countAgg.asText()).isEqualTo("@timestamp");
    }
}
