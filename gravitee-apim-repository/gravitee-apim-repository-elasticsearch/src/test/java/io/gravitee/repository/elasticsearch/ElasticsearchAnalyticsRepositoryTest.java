/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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
package io.gravitee.repository.elasticsearch;

import static io.gravitee.repository.analytics.query.DateRangeBuilder.lastDays;
import static io.gravitee.repository.analytics.query.IntervalBuilder.hours;
import static io.gravitee.repository.analytics.query.QueryBuilders.*;
import static java.lang.Float.valueOf;

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
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ElasticsearchAnalyticsRepositoryTest extends AbstractElasticsearchRepositoryTest {

    @Autowired
    private AnalyticsRepository analyticsRepository;

    @Test
    public void testDateHistogram() throws Exception {
        Assert.assertNotNull(analyticsRepository);

        DateHistogramResponse response = analyticsRepository.query(dateHistogram().timeRange(lastDays(30), hours(1)).build());

        Assert.assertNotNull(response);
    }

    @Test
    public void testDateHistogram_root() throws Exception {
        Assert.assertNotNull(analyticsRepository);

        DateHistogramResponse response = analyticsRepository.query(
            dateHistogram().timeRange(lastDays(90), hours(1)).root("api", "be0aa9c9-ca1c-4d0a-8aa9-c9ca1c5d0aab").build()
        );

        Assert.assertNotNull(response);
    }

    @Test
    public void testDateHistogram_singleAggregation() throws Exception {
        Assert.assertNotNull(analyticsRepository);

        DateHistogramResponse response = analyticsRepository.query(
            dateHistogram().timeRange(lastDays(60), hours(1)).aggregation(AggregationType.AVG, "response-time").build()
        );

        Assert.assertNotNull(response);
    }

    @Test
    public void testDateHistogram_multipleAggregation_average() throws Exception {
        Assert.assertNotNull(analyticsRepository);

        DateHistogramResponse response = analyticsRepository.query(
            dateHistogram()
                .timeRange(lastDays(30), hours(1))
                .query("api:be0aa9c9-ca1c-4d0a-8aa9-c9ca1c5d0aab")
                .aggregation(AggregationType.AVG, "response-time")
                .aggregation(AggregationType.AVG, "api-response-time")
                .build()
        );

        Assert.assertNotNull(response);
    }

    @Test
    public void testDateHistogram_multipleAggregation_averageAndField() throws Exception {
        Assert.assertNotNull(analyticsRepository);

        DateHistogramResponse response = analyticsRepository.query(
            dateHistogram()
                .timeRange(lastDays(30), hours(1))
                .query("api:be0aa9c9-ca1c-4d0a-8aa9-c9ca1c5d0aab")
                .aggregation(AggregationType.AVG, "response-time")
                .aggregation(AggregationType.FIELD, "application")
                .build()
        );

        Assert.assertNotNull(response);
    }

    @Test
    public void testGroupBy_simpleField() throws Exception {
        Assert.assertNotNull(analyticsRepository);

        GroupByResponse response = analyticsRepository.query(
            groupBy().timeRange(lastDays(60), hours(1)).query("api:be0aa9c9-ca1c-4d0a-8aa9-c9ca1c5d0aab").field("application").build()
        );

        Assert.assertNotNull(response);
    }

    @Test
    public void testGroupBy_simpleField_withOrder() throws Exception {
        Assert.assertNotNull(analyticsRepository);

        GroupByResponse response = analyticsRepository.query(
            groupBy()
                .timeRange(lastDays(30), hours(1))
                .query("api:be0aa9c9-ca1c-4d0a-8aa9-c9ca1c5d0aab")
                .field("application")
                .sort(SortBuilder.on("response-time", Order.DESC, SortType.AVG))
                .build()
        );

        Assert.assertNotNull(response);
    }

    @Test
    public void testGroupBy_simpleField_withRanges() throws Exception {
        Assert.assertNotNull(analyticsRepository);

        GroupByResponse response = analyticsRepository.query(
            groupBy()
                .timeRange(lastDays(30), hours(1))
                .query("api:be0aa9c9-ca1c-4d0a-8aa9-c9ca1c5d0aab")
                .field("status")
                .range(100, 199)
                .range(200, 299)
                .range(300, 399)
                .range(400, 499)
                .build()
        );

        Assert.assertNotNull(response);
        for (Bucket bucket : response.getValues()) {
            if (bucket.name().startsWith("100")) {
                Assert.assertEquals(0, bucket.value());
            }
            if (bucket.name().startsWith("200")) {
                Assert.assertEquals(1, bucket.value()); //line 56 in bulk.json
            }
            if (bucket.name().startsWith("300")) {
                Assert.assertEquals(0, bucket.value());
            }
            if (bucket.name().startsWith("400")) {
                Assert.assertEquals(1, bucket.value()); //line 42 in bulk.json
            }
        }
    }

    @Test
    public void testCount() throws Exception {
        Assert.assertNotNull(analyticsRepository);

        CountResponse response = analyticsRepository.query(
            count().timeRange(lastDays(30), hours(1)).query("api:4d8d6ca8-c2c7-4ab8-8d6c-a8c2c79ab8a1").build()
        );

        Assert.assertNotNull(response);
        Assert.assertEquals(3, response.getCount());
    }

    @Test
    public void testCountByPath() throws Exception {
        Assert.assertNotNull(analyticsRepository);

        CountResponse response = analyticsRepository.query(count().timeRange(lastDays(30), hours(1)).query("(path:/mypath)").build());

        Assert.assertNotNull(response);
        Assert.assertEquals(1, response.getCount());
    }

    @Test
    public void testCountByHost() throws Exception {
        Assert.assertNotNull(analyticsRepository);

        CountResponse response = analyticsRepository.query(
            count().timeRange(lastDays(30), hours(1)).query("(host:localhost:8082)").build()
        );

        Assert.assertNotNull(response);
        Assert.assertEquals(1, response.getCount());
    }

    @Test
    public void testCountByHostAndPath() throws Exception {
        Assert.assertNotNull(analyticsRepository);

        CountResponse response = analyticsRepository.query(
            count().timeRange(lastDays(30), hours(1)).query("((path:/mypath) AND (host:localhost:8082))").build()
        );

        Assert.assertNotNull(response);
        Assert.assertEquals(1, response.getCount());
    }

    @Test
    public void testStats() throws Exception {
        Assert.assertNotNull(analyticsRepository);

        final StatsResponse response = analyticsRepository.query(
            stats().timeRange(lastDays(30), hours(1)).query("api:4d8d6ca8-c2c7-4ab8-8d6c-a8c2c79ab8a1").field("response-time").build()
        );

        Assert.assertNotNull(response);
        Assert.assertEquals(valueOf(3), response.getCount());
        Assert.assertEquals(valueOf(2), response.getMin());
        Assert.assertEquals(valueOf(51), response.getMax());
        Assert.assertEquals(valueOf(32.333332f), response.getAvg());
        Assert.assertEquals(valueOf(97), response.getSum());
    }
}
