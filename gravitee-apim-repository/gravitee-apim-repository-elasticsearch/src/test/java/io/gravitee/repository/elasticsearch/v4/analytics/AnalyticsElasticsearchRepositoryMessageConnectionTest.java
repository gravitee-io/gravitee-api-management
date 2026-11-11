/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.elasticsearch.v4.analytics;

import static io.gravitee.definition.model.DefinitionVersion.V4;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.analytics.engine.api.metric.Measure;
import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.Filter;
import io.gravitee.repository.analytics.engine.api.query.MeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.elasticsearch.AbstractElasticsearchRepositoryTest;
import io.gravitee.repository.elasticsearch.TimeProvider;
import io.gravitee.repository.log.v4.model.analytics.RequestResponseTimeQueryCriteria;
import io.gravitee.repository.log.v4.model.analytics.RequestsCountQuery;
import io.gravitee.repository.log.v4.model.analytics.ResponseTimeRangeQuery;
import io.gravitee.repository.log.v4.model.analytics.SearchTermId;
import io.gravitee.repository.log.v4.model.analytics.StatsQuery;
import io.gravitee.repository.log.v4.model.analytics.TimeRange;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/**
 * One message API, two connections over http-get: one ended — an opening and an end of 1000 ms — and one still open,
 * reported at opening only. Both are counted as they open; durations are those of the ended one alone.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@TestPropertySource(properties = "reporters.elasticsearch.template_mapping.path=src/test/resources/freemarker-v4-analytics")
class AnalyticsElasticsearchRepositoryMessageConnectionTest extends AbstractElasticsearchRepositoryTest {

    private static final String MESSAGE_API = "c3d4e5f6-0000-4000-8000-0000000000e5";
    private static final QueryContext QUERY_CONTEXT = new QueryContext("org#1", "env#1");
    private static final Instant TODAY = TimeProvider.now().truncatedTo(ChronoUnit.DAYS);
    private static final Instant TOMORROW = TODAY.plus(Duration.ofDays(1));

    @Autowired
    private AnalyticsElasticsearchRepository cut;

    @Test
    void should_count_a_connection_still_open() {
        var result = cut.searchRequestsCount(QUERY_CONTEXT, new RequestsCountQuery(MESSAGE_API));

        assertThat(result).hasValueSatisfying(count -> {
            assertThat(count.getTotal()).isEqualTo(2);
            assertThat(count.getCountBy()).containsExactlyEntriesOf(Map.of("http-get", 2L));
        });
    }

    @Test
    void should_count_both_connections_and_time_the_ended_one_only() {
        var result = cut.searchRequestResponseTimes(
            QUERY_CONTEXT,
            new RequestResponseTimeQueryCriteria(List.of(MESSAGE_API), TODAY.toEpochMilli(), TOMORROW.toEpochMilli(), EnumSet.of(V4))
        );

        assertThat(result.getRequestsTotal()).isEqualTo(2L);
        assertThat(result.getResponseMinTime()).isEqualTo(1000.0);
        assertThat(result.getResponseMaxTime()).isEqualTo(1000.0);
        assertThat(result.getResponseAvgTime()).isEqualTo(1000.0);
    }

    @Test
    void should_count_both_connections_in_stats_and_measure_the_ended_one_only() {
        var query = new StatsQuery(
            "gateway-response-time-ms",
            new SearchTermId(SearchTermId.SearchTerm.API, MESSAGE_API),
            new TimeRange(TODAY, TOMORROW),
            Optional.empty()
        );

        assertThat(cut.searchStats(QUERY_CONTEXT, query)).hasValueSatisfying(stats -> {
            assertThat(stats.count()).isEqualTo(2.0f);
            assertThat(stats.min()).isEqualTo(1000.0f);
            assertThat(stats.max()).isEqualTo(1000.0f);
            assertThat(stats.avg()).isEqualTo(1000.0f);
            assertThat(stats.sum()).isEqualTo(1000.0f);
        });
    }

    @Test
    void should_average_the_response_time_over_ended_connections_only() {
        var result = cut
            .searchResponseTimeOverTime(
                QUERY_CONTEXT,
                new ResponseTimeRangeQuery(List.of(MESSAGE_API), TODAY, TOMORROW, Duration.ofHours(1))
            )
            .blockingGet();

        assertThat(result.getAverageBy().entrySet())
            .filteredOn(bucket -> bucket.getKey().endsWith("T08:00:00.000Z"))
            .singleElement()
            .satisfies(bucket -> assertThat(bucket.getValue()).isEqualTo(1000.0));
    }

    @Test
    void should_count_both_connections_and_average_the_ended_one_in_the_engine() {
        var result = cut.searchHTTPMeasures(
            QUERY_CONTEXT,
            new MeasuresQuery(
                new io.gravitee.repository.analytics.engine.api.query.TimeRange(TODAY, TOMORROW),
                List.of(new Filter(Filter.Name.API, Filter.Operator.IN, List.of(MESSAGE_API))),
                List.of(
                    new MetricMeasuresQuery(Metric.HTTP_REQUESTS, Set.of(Measure.COUNT)),
                    new MetricMeasuresQuery(Metric.HTTP_GATEWAY_RESPONSE_TIME, Set.of(Measure.AVG, Measure.MIN))
                )
            )
        );

        var measures = result.measures();
        assertThat(measures)
            .filteredOn(metric -> metric.metric() == Metric.HTTP_REQUESTS)
            .singleElement()
            .satisfies(metric -> assertThat(metric.measures().get(Measure.COUNT).longValue()).isEqualTo(2L));
        assertThat(measures)
            .filteredOn(metric -> metric.metric() == Metric.HTTP_GATEWAY_RESPONSE_TIME)
            .singleElement()
            .satisfies(metric -> {
                assertThat(metric.measures().get(Measure.AVG).doubleValue()).isEqualTo(1000.0);
                assertThat(metric.measures().get(Measure.MIN).doubleValue()).isEqualTo(1000.0);
            });
    }
}
