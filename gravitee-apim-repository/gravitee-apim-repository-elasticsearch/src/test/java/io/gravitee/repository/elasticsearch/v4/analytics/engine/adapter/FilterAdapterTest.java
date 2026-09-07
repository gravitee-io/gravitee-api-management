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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.repository.analytics.engine.api.metric.Measure;
import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.Filter;
import io.gravitee.repository.analytics.engine.api.query.MeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.TimeRange;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
class FilterAdapterTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final Long FROM = 1756104349879L;
    private static final Long TO = 1756190749879L;

    private final HTTPMeasuresQueryAdapter measuresAdapter = new HTTPMeasuresQueryAdapter();

    private TimeRange buildTimeRange() {
        return new TimeRange(Instant.ofEpochMilli(FROM), Instant.ofEpochMilli(TO));
    }

    @Nested
    class HttpMethodFilters {

        @Test
        void should_filter_on_the_reported_method_code_for_eq() throws JsonProcessingException {
            var filters = List.of(new Filter(Filter.Name.HTTP_METHOD, Filter.Operator.EQ, "GET"));
            var metrics = List.of(new MetricMeasuresQuery(Metric.HTTP_REQUESTS, Set.of(Measure.COUNT)));
            var query = new MeasuresQuery(buildTimeRange(), filters, metrics);

            var jsonQuery = JSON.readTree(measuresAdapter.adapt(query));

            var term = jsonQuery.at("/query/bool/filter/1/term/http-method");
            assertThat(term.isIntegralNumber()).isTrue();
            assertThat(term.asInt()).isEqualTo(3);
        }

        @Test
        void should_filter_on_the_reported_method_codes_for_in() throws JsonProcessingException {
            var filters = List.of(new Filter(Filter.Name.HTTP_METHOD, Filter.Operator.IN, List.of("POST", "put")));
            var metrics = List.of(new MetricMeasuresQuery(Metric.HTTP_REQUESTS, Set.of(Measure.COUNT)));
            var query = new MeasuresQuery(buildTimeRange(), filters, metrics);

            var jsonQuery = JSON.readTree(measuresAdapter.adapt(query));

            var terms = jsonQuery.at("/query/bool/filter/1/terms/http-method");
            assertThat(terms.isArray()).isTrue();
            assertThat(terms.get(0).asInt()).isEqualTo(7);
            assertThat(terms.get(1).asInt()).isEqualTo(8);
        }

        @Test
        void should_throw_for_an_unknown_http_method() {
            var filters = List.of(new Filter(Filter.Name.HTTP_METHOD, Filter.Operator.EQ, "FETCH"));
            var metrics = List.of(new MetricMeasuresQuery(Metric.HTTP_REQUESTS, Set.of(Measure.COUNT)));
            var query = new MeasuresQuery(buildTimeRange(), filters, metrics);

            assertThatThrownBy(() -> measuresAdapter.adapt(query))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FETCH");
        }
    }
}
