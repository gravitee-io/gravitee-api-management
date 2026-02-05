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

        var latencyP90 = aggs.at("/HTTP_GATEWAY_LATENCY#P90/percentiles/percents/0").asDouble();
        assertThat(latencyP90).isEqualTo(90.0);

        var latencyP95 = aggs.at("/HTTP_GATEWAY_LATENCY#P95/percentiles/percents/0").asDouble();
        assertThat(latencyP95).isEqualTo(95.0);

        var latencyP99 = aggs.at("/HTTP_GATEWAY_LATENCY#P99/percentiles/percents/0").asDouble();
        assertThat(latencyP99).isEqualTo(99.0);

        var gatewayP90 = aggs.at("/HTTP_GATEWAY_RESPONSE_TIME#P90/percentiles/percents/0").asDouble();
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

        var avgAgg = aggs.at("/HTTP_GATEWAY_LATENCY#AVG/avg/field");
        assertThat(avgAgg).isNotNull();
        assertThat(avgAgg.asText()).isEqualTo("gateway-latency-ms");
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

        var countAgg = aggs.at("/HTTP_REQUESTS#COUNT/value_count/field");
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

        var maxAgg = aggs.at("/HTTP_GATEWAY_LATENCY#MAX/max/field");
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

        var minAgg = aggs.at("/HTTP_GATEWAY_LATENCY#MIN/min/field");
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

        var p50Agg = aggs.at("/HTTP_GATEWAY_LATENCY#P50/percentiles/percents/0").asDouble();
        assertThat(p50Agg).isEqualTo(50.0);
    }

    @Test
    void should_build_query_with_rps_measure() throws JsonProcessingException {
        var timeRange = buildTimeRange();
        var filters = buildFilters();
        var metrics = List.of(new MetricMeasuresQuery(Metric.HTTP_REQUESTS, Set.of(Measure.RPS)));

        var query = new MeasuresQuery(timeRange, filters, metrics);

        var queryString = adapter.adapt(query);

        var jsonQuery = JSON.readTree(queryString);

        var aggs = jsonQuery.at("/aggs");
        assertThat(aggs).isNotEmpty();

        // RPS uses a date_histogram with bucket_script
        var rpsDateHistogram = aggs.at("/_HTTP_REQUESTS#RPS/date_histogram");
        assertThat(rpsDateHistogram).isNotNull();

        var rpsBucketScript = aggs.at("/_HTTP_REQUESTS#RPS/aggs/HTTP_REQUESTS#RPS/bucket_script");
        assertThat(rpsBucketScript).isNotNull();
        assertThat(rpsBucketScript.has("script")).isTrue();
    }

    @Test
    void should_build_query_with_error_rate_measure() throws JsonProcessingException {
        var timeRange = buildTimeRange();
        var filters = buildFilters();
        var metrics = List.of(new MetricMeasuresQuery(Metric.HTTP_ERRORS, Set.of(Measure.PERCENTAGE)));

        var query = new MeasuresQuery(timeRange, filters, metrics);

        var queryString = adapter.adapt(query);

        var jsonQuery = JSON.readTree(queryString);

        var aggs = jsonQuery.at("/aggs");
        assertThat(aggs).isNotEmpty();

        // Error rate uses a date_histogram with bucket_script
        var errorRateDateHistogram = aggs.at("/_HTTP_ERRORS#PERCENTAGE/date_histogram");
        assertThat(errorRateDateHistogram).isNotNull();

        var errorRateBucketScript = aggs.at("/_HTTP_ERRORS#PERCENTAGE/aggs/HTTP_ERRORS#PERCENTAGE/bucket_script");
        assertThat(errorRateBucketScript).isNotNull();
        assertThat(errorRateBucketScript.has("script")).isTrue();
    }

    @Test
    void should_build_query_with_llm_total_token_count_measure() throws JsonProcessingException {
        var timeRange = buildTimeRange();
        var filters = buildFilters();
        var metrics = List.of(new MetricMeasuresQuery(Metric.LLM_PROMPT_TOTAL_TOKEN, Set.of(Measure.COUNT)));

        var query = new MeasuresQuery(timeRange, filters, metrics);

        var queryString = adapter.adapt(query);

        var jsonQuery = JSON.readTree(queryString);

        var aggs = jsonQuery.at("/aggs");
        assertThat(aggs).isNotEmpty();

        // LLM_PROMPT_TOTAL_TOKEN COUNT uses a sum aggregation with script
        var sumAgg = aggs.at("/LLM_PROMPT_TOTAL_TOKEN#COUNT/sum/script/source");
        assertThat(sumAgg).isNotNull();
        assertThat(sumAgg.asText()).contains("additional-metrics.long_llm-proxy_tokens-sent");
        assertThat(sumAgg.asText()).contains("additional-metrics.long_llm-proxy_tokens-received");
    }

    @Test
    void should_build_query_with_llm_total_token_avg_measure() throws JsonProcessingException {
        var timeRange = buildTimeRange();
        var filters = buildFilters();
        var metrics = List.of(new MetricMeasuresQuery(Metric.LLM_PROMPT_TOTAL_TOKEN, Set.of(Measure.AVG)));

        var query = new MeasuresQuery(timeRange, filters, metrics);

        var queryString = adapter.adapt(query);

        var jsonQuery = JSON.readTree(queryString);

        var aggs = jsonQuery.at("/aggs");
        assertThat(aggs).isNotEmpty();

        // LLM_PROMPT_TOTAL_TOKEN AVG uses an avg aggregation with script
        var avgAgg = aggs.at("/LLM_PROMPT_TOTAL_TOKEN#AVG/avg/script/source");
        assertThat(avgAgg).isNotNull();
        assertThat(avgAgg.asText()).contains("additional-metrics.long_llm-proxy_tokens-sent");
        assertThat(avgAgg.asText()).contains("additional-metrics.long_llm-proxy_tokens-received");
    }

    @Test
    void should_build_query_with_llm_total_cost_count_measure() throws JsonProcessingException {
        var timeRange = buildTimeRange();
        var filters = buildFilters();
        var metrics = List.of(new MetricMeasuresQuery(Metric.LLM_PROMPT_TOKEN_COST, Set.of(Measure.COUNT)));

        var query = new MeasuresQuery(timeRange, filters, metrics);

        var queryString = adapter.adapt(query);

        var jsonQuery = JSON.readTree(queryString);

        var aggs = jsonQuery.at("/aggs");
        assertThat(aggs).isNotEmpty();

        var sumAgg = aggs.at("/LLM_PROMPT_TOKEN_COST#COUNT/sum/script/source");
        assertThat(sumAgg).isNotNull();
        assertThat(sumAgg.asText()).contains("additional-metrics.double_llm-proxy_sent-cost");
        assertThat(sumAgg.asText()).contains("additional-metrics.double_llm-proxy_received-cost");
    }

    @Test
    void should_build_query_with_llm_total_cost_avg_measure() throws JsonProcessingException {
        var timeRange = buildTimeRange();
        var filters = buildFilters();
        var metrics = List.of(new MetricMeasuresQuery(Metric.LLM_PROMPT_TOKEN_COST, Set.of(Measure.AVG)));

        var query = new MeasuresQuery(timeRange, filters, metrics);

        var queryString = adapter.adapt(query);

        var jsonQuery = JSON.readTree(queryString);

        var aggs = jsonQuery.at("/aggs");
        assertThat(aggs).isNotEmpty();

        var avgAgg = aggs.at("/LLM_PROMPT_TOKEN_COST#AVG/avg/script/source");
        assertThat(avgAgg).isNotNull();
        assertThat(avgAgg.asText()).contains("additional-metrics.double_llm-proxy_sent-cost");
        assertThat(avgAgg.asText()).contains("additional-metrics.double_llm-proxy_received-cost");
    }
}
