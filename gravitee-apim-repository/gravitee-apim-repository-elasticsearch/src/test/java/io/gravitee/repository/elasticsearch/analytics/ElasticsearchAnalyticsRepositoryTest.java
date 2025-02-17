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
package io.gravitee.repository.elasticsearch.analytics;

import static io.gravitee.repository.analytics.query.DateRangeBuilder.lastDays;
import static io.gravitee.repository.analytics.query.IntervalBuilder.hours;
import static io.gravitee.repository.analytics.query.QueryBuilders.count;
import static io.gravitee.repository.analytics.query.QueryBuilders.dateHistogram;
import static io.gravitee.repository.analytics.query.QueryBuilders.groupBy;
import static io.gravitee.repository.analytics.query.QueryBuilders.stats;
import static java.lang.Float.valueOf;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.analytics.api.AnalyticsRepository;
import io.gravitee.repository.analytics.query.AggregationType;
import io.gravitee.repository.analytics.query.Order;
import io.gravitee.repository.analytics.query.SortBuilder;
import io.gravitee.repository.analytics.query.SortType;
import io.gravitee.repository.analytics.query.count.CountResponse;
import io.gravitee.repository.analytics.query.groupby.GroupByResponse;
import io.gravitee.repository.analytics.query.groupby.GroupByResponse.Bucket;
import io.gravitee.repository.analytics.query.response.histogram.DateHistogramResponse;
import io.gravitee.repository.analytics.query.stats.StatsResponse;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.elasticsearch.AbstractElasticsearchRepositoryTest;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ElasticsearchAnalyticsRepositoryTest extends AbstractElasticsearchRepositoryTest {

    private final QueryContext queryContext = new QueryContext("org#1", "env#1");

    @Autowired
    private AnalyticsRepository analyticsRepository;

    @Test
    public void testDateHistogram() throws Exception {
        DateHistogramResponse response = analyticsRepository.query(queryContext, dateHistogram().timeRange(lastDays(30), hours(1)).build());

        assertThat(response).isNotNull();
    }

    @Test
    public void testDateHistogram_root() throws Exception {
        DateHistogramResponse response = analyticsRepository.query(
            queryContext,
            dateHistogram().timeRange(lastDays(90), hours(1)).root("api", "be0aa9c9-ca1c-4d0a-8aa9-c9ca1c5d0aab").build()
        );

        assertThat(response).isNotNull();
    }

    @Test
    public void testDateHistogram_singleAggregation() throws Exception {
        DateHistogramResponse response = analyticsRepository.query(
            queryContext,
            dateHistogram().timeRange(lastDays(60), hours(1)).aggregation(AggregationType.AVG, "response-time").build()
        );

        assertThat(response).isNotNull();
    }

    @Test
    public void testDateHistogram_multipleAggregation_average() throws Exception {
        DateHistogramResponse response = analyticsRepository.query(
            queryContext,
            dateHistogram()
                .timeRange(lastDays(30), hours(1))
                .terms(Map.of("api", Set.of("be0aa9c9-ca1c-4d0a-8aa9-c9ca1c5d0aab")))
                .aggregation(AggregationType.AVG, "response-time")
                .aggregation(AggregationType.AVG, "api-response-time")
                .build()
        );

        assertThat(response).isNotNull();
    }

    @Test
    public void testDateHistogram_multipleAggregation_averageAndField() throws Exception {
        DateHistogramResponse response = analyticsRepository.query(
            queryContext,
            dateHistogram()
                .timeRange(lastDays(30), hours(1))
                .terms(Map.of("api", Set.of("be0aa9c9-ca1c-4d0a-8aa9-c9ca1c5d0aab")))
                .aggregation(AggregationType.AVG, "response-time")
                .aggregation(AggregationType.FIELD, "application")
                .build()
        );

        assertThat(response).isNotNull();
    }

    @Test
    public void testGroupBy_simpleField() throws Exception {
        GroupByResponse response = analyticsRepository.query(
            queryContext,
            groupBy()
                .timeRange(lastDays(60), hours(1))
                .terms(Map.of("api", Set.of("be0aa9c9-ca1c-4d0a-8aa9-c9ca1c5d0aab")))
                .field("application")
                .build()
        );

        assertThat(response).isNotNull();
    }

    @Test
    public void testGroupBy_simpleField_withOrder() throws Exception {
        GroupByResponse response = analyticsRepository.query(
            queryContext,
            groupBy()
                .timeRange(lastDays(30), hours(1))
                .terms(Map.of("api", Set.of("be0aa9c9-ca1c-4d0a-8aa9-c9ca1c5d0aab")))
                .field("application")
                .sort(SortBuilder.on("response-time", Order.DESC, SortType.AVG))
                .build()
        );

        assertThat(response).isNotNull();
    }

    @Test
    public void testGroupBy_simpleField_withRanges() throws Exception {
        GroupByResponse response = analyticsRepository.query(
            queryContext,
            groupBy()
                .timeRange(lastDays(30), hours(1))
                .terms(Map.of("api", Set.of("be0aa9c9-ca1c-4d0a-8aa9-c9ca1c5d0aab")))
                .field("status")
                .range(100, 199)
                .range(200, 299)
                .range(300, 399)
                .range(400, 499)
                .build()
        );

        assertThat(response).isNotNull();
        for (Bucket bucket : response.getValues()) {
            if (bucket.name().startsWith("100")) {
                assertThat(bucket.value()).isZero();
            }
            if (bucket.name().startsWith("200")) {
                assertThat(bucket.value()).isEqualTo(6); //line 56 in bulk.ftl
            }
            if (bucket.name().startsWith("300")) {
                assertThat(bucket.value()).isZero();
            }
            if (bucket.name().startsWith("400")) {
                assertThat(bucket.value()).isOne(); //line 42 in bulk.ftl
            }
        }
    }

    @Test
    public void testCount() throws Exception {
        CountResponse response = analyticsRepository.query(
            queryContext,
            count().timeRange(lastDays(30), hours(1)).terms(Map.of("api", Set.of("4d8d6ca8-c2c7-4ab8-8d6c-a8c2c79ab8a1"))).build()
        );

        assertThat(response).isNotNull();
        assertThat(response.getCount()).isEqualTo(3);
    }

    @Test
    public void testCountWithTwoApis() throws Exception {
        CountResponse response = analyticsRepository.query(
            queryContext,
            count()
                .timeRange(lastDays(30), hours(1))
                .terms(Map.of("api", Set.of("4d8d6ca8-c2c7-4ab8-8d6c-a8c2c79ab8a1", "e2c0ecd5-893a-458d-80ec-d5893ab58d12")))
                .build()
        );

        assertThat(response).isNotNull();
        assertThat(response.getCount()).isEqualTo(7);
    }

    @Test
    public void testCountByPath() throws Exception {
        CountResponse response = analyticsRepository.query(
            queryContext,
            count().timeRange(lastDays(30), hours(1)).query("(path:/mypath)").build()
        );

        assertThat(response).isNotNull();
        assertThat(response.getCount()).isOne();
    }

    @Test
    public void testCountByHost() throws Exception {
        CountResponse response = analyticsRepository.query(
            queryContext,
            count().timeRange(lastDays(30), hours(1)).query("(host:localhost:8082)").build()
        );

        assertThat(response).isNotNull();
        assertThat(response.getCount()).isOne();
    }

    @Test
    public void testCountByHostAndPath() throws Exception {
        CountResponse response = analyticsRepository.query(
            queryContext,
            count().timeRange(lastDays(30), hours(1)).query("((path:/mypath) AND (host:localhost:8082))").build()
        );

        assertThat(response).isNotNull();
        assertThat(response.getCount()).isOne();
    }

    @Test
    public void testStats() throws Exception {
        final StatsResponse response = analyticsRepository.query(
            queryContext,
            stats()
                .timeRange(lastDays(30), hours(1))
                .terms(Map.of("api", Set.of("4d8d6ca8-c2c7-4ab8-8d6c-a8c2c79ab8a1")))
                .field("response-time")
                .build()
        );

        assertThat(response).isNotNull();
        assertThat(response.getCount()).isEqualTo(valueOf(3));
        assertThat(response.getMin()).isEqualTo(valueOf(2));
        assertThat(response.getMax()).isEqualTo(valueOf(51));
        assertThat(response.getAvg()).isEqualTo(valueOf(32.333332f));
        assertThat(response.getSum()).isEqualTo(valueOf(97));
    }
}
