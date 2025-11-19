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
import io.gravitee.repository.analytics.engine.api.query.Facet;
import io.gravitee.repository.analytics.engine.api.query.FacetsQuery;
import io.gravitee.repository.analytics.engine.api.query.NumberRange;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class HTTPFacetsQueryAdapterTest extends AbstractQueryAdapterTest {

    final HTTPFacetsQueryAdapter adapter = new HTTPFacetsQueryAdapter();

    @Test
    void should_build_query() throws JsonProcessingException {
        var timeRange = buildTimeRange();
        var filters = buildFilters();
        var metrics = buildMetrics();

        var facets = List.of(Facet.API, Facet.APPLICATION);

        var query = new FacetsQuery(timeRange, filters, metrics, facets);
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

        var latencyP90 = aggs
            .at("/HTTP_GATEWAY_LATENCY#API/aggs/HTTP_GATEWAY_LATENCY#APPLICATION/aggs/HTTP_GATEWAY_LATENCY#P90/percentiles/percents/0")
            .asDouble();
        assertThat(latencyP90).isEqualTo(90.0);

        var gatewayP90 = aggs
            .at(
                "/HTTP_GATEWAY_RESPONSE_TIME#API/aggs/HTTP_GATEWAY_RESPONSE_TIME#APPLICATION/aggs/HTTP_GATEWAY_RESPONSE_TIME#P90/percentiles/percents/0"
            )
            .asDouble();
        assertThat(gatewayP90).isEqualTo(90.0);
    }

    @Test
    void should_build_query_with_sorts() throws JsonProcessingException {
        var timeRange = buildTimeRange();
        var filters = buildFilters();
        var metrics = buildSortedMetric();

        var facets = List.of(Facet.API);
        var limit = 2;

        var query = new FacetsQuery(timeRange, filters, metrics, facets, limit);

        var queryString = adapter.adapt(query);
        var jsonQuery = JSON.readTree(queryString);

        System.out.println(queryString);

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

        var terms = aggs.at("/HTTP_REQUESTS#API/terms");
        assertThat(terms).isNotEmpty();

        var field = terms.at("/field").asText();
        assertThat(field).isEqualTo("api-id");

        var size = terms.at("/size").asInt();
        assertThat(size).isEqualTo(limit);

        var order = terms.at("/order/HTTP_REQUESTS#COUNT").asText();
        assertThat(order).isEqualTo("desc");
    }

    @Test
    void should_build_query_with_ranges() throws JsonProcessingException {
        var timeRange = buildTimeRange();
        var filters = buildFilters();
        var metrics = buildMetrics();

        var facets = List.of(Facet.HTTP_STATUS);
        var ranges = List.of(
            new NumberRange(100, 199),
            new NumberRange(200, 299),
            new NumberRange(300, 399),
            new NumberRange(400, 499),
            new NumberRange(500, 599)
        );

        var query = new FacetsQuery(timeRange, filters, metrics, facets, ranges);
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

        var field = aggs.at("/HTTP_GATEWAY_LATENCY#HTTP_STATUS/range/field").asText();
        assertThat(field).isEqualTo("status");

        var esRanges = aggs.at("/HTTP_GATEWAY_LATENCY#HTTP_STATUS/range/ranges");
        assertThat(esRanges).isNotEmpty();
    }

    @Test
    void should_build_query_for_status_code_group_with_ranges() throws JsonProcessingException {
        var timeRange = buildTimeRange();
        var filters = buildFilters();
        var metrics = buildMetrics();

        var facets = List.of(Facet.HTTP_STATUS_CODE_GROUP);

        var query = new FacetsQuery(timeRange, filters, metrics, facets);
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

        var field = aggs.at("/HTTP_GATEWAY_LATENCY#HTTP_STATUS_CODE_GROUP/range/field").asText();
        assertThat(field).isEqualTo("status");

        var esRanges = aggs.at("/HTTP_GATEWAY_LATENCY#HTTP_STATUS_CODE_GROUP/range/ranges");
        assertThat(esRanges).isNotEmpty();
    }
}
