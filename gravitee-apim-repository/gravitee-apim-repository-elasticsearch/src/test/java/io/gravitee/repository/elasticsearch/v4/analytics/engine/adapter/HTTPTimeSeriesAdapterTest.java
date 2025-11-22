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
import io.gravitee.repository.analytics.engine.api.query.TimeSeriesQuery;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HTTPTimeSeriesAdapterTest extends AbstractQueryAdapterTest {

    final HTTPTimeSeriesQueryAdapter adapter = new HTTPTimeSeriesQueryAdapter();

    @Test
    void should_build_query() throws JsonProcessingException {
        var timeRange = buildTimeRange();
        var filters = buildFilters();
        var metrics = buildMetrics();

        var facets = List.of(Facet.HTTP_STATUS);

        var interval = Duration.ofHours(1).toMillis();

        var query = new TimeSeriesQuery(timeRange, filters, interval, metrics, facets);
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

        var gatewayLatencyDateHistogram = aggs.at("/HTTP_GATEWAY_LATENCY#TIME_SERIES/date_histogram");
        assertThat(gatewayLatencyDateHistogram).isNotEmpty();

        var gatewayResponseTimeDateHistogram = aggs.at("/HTTP_GATEWAY_RESPONSE_TIME#TIME_SERIES/date_histogram");
        assertThat(gatewayResponseTimeDateHistogram).isNotEmpty();
    }
}
