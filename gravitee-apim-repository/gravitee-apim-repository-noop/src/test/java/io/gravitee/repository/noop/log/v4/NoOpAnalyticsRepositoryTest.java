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
package io.gravitee.repository.noop.log.v4;

import static io.gravitee.definition.model.DefinitionVersion.V4;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.log.v4.api.AnalyticsRepository;
import io.gravitee.repository.log.v4.model.analytics.AverageConnectionDurationQuery;
import io.gravitee.repository.log.v4.model.analytics.AverageMessagesPerRequestQuery;
import io.gravitee.repository.log.v4.model.analytics.RequestResponseTimeQueryCriteria;
import io.gravitee.repository.log.v4.model.analytics.RequestsCountByEventQuery;
import io.gravitee.repository.log.v4.model.analytics.RequestsCountQuery;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusOverTimeQuery;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusQueryCriteria;
import io.gravitee.repository.log.v4.model.analytics.ResponseTimeRangeQuery;
import io.gravitee.repository.log.v4.model.analytics.SearchTermId;
import io.gravitee.repository.log.v4.model.analytics.TimeRange;
import io.gravitee.repository.log.v4.model.analytics.TopHitsQueryCriteria;
import io.gravitee.repository.noop.AbstractNoOpRepositoryTest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class NoOpAnalyticsRepositoryTest extends AbstractNoOpRepositoryTest {

    @Autowired
    private AnalyticsRepository analyticsRepository;

    private final QueryContext queryContext = new QueryContext("org#1", "env#1");
    private static final String API_ID = "f1608475-dd77-4603-a084-75dd775603e9";

    @Test
    public void testSearchRequestsCount() throws Exception {
        Assert.assertNotNull(analyticsRepository);

        var result = analyticsRepository.searchRequestsCount(queryContext, new RequestsCountQuery(API_ID));

        assertNotNull(result);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testSearchAverageMessagesPerRequest() {
        var result = analyticsRepository.searchAverageMessagesPerRequest(queryContext, new AverageMessagesPerRequestQuery(API_ID));

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSearchAverageConnectionDuration() {
        var result = analyticsRepository.searchAverageConnectionDuration(queryContext, new AverageConnectionDurationQuery(API_ID));
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSearchResponseStatusRanges() {
        var result = analyticsRepository.searchResponseStatusRanges(
            queryContext,
            ResponseStatusQueryCriteria.builder().apiIds(List.of(API_ID)).build()
        );
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSearchTopHitsApi() {
        var yesterdayAtStartOfTheDayEpochMilli = LocalDate.now().minusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
        var yesterdayAtEndOfTheDayEpochMilli = LocalDate.now().minusDays(1).atTime(23, 59, 59).toInstant(ZoneOffset.UTC).toEpochMilli();

        var result = analyticsRepository.searchTopHitsApi(
            queryContext,
            new TopHitsQueryCriteria(List.of(API_ID), yesterdayAtStartOfTheDayEpochMilli, yesterdayAtEndOfTheDayEpochMilli)
        );
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSearchResponseTimeOverTime() {
        var now = Instant.now();
        var from = now.minus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS);
        var to = from.plus(Duration.ofDays(1));
        Duration interval = Duration.ofMinutes(10);
        var result = analyticsRepository.searchResponseTimeOverTime(
            queryContext,
            new ResponseTimeRangeQuery(List.of("f1608475-dd77-4603-a084-75dd775603e9"), from, to, interval)
        );
        assertNotNull(result);
    }

    @Test
    public void testSearchResponseStatusOvertime() {
        var now = Instant.now();
        var from = now.minus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS);
        var to = now.plus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS);
        var interval = Duration.ofMinutes(30);
        var result = analyticsRepository.searchResponseStatusOvertime(
            queryContext,
            ResponseStatusOverTimeQuery.builder().apiIds(List.of(API_ID)).from(from).to(to).interval(interval).build()
        );

        // Verify that the result is null
        assertNotNull(result);
    }

    @Test
    public void testSearchRequestResponseTimes() {
        var now = Instant.now();
        var from = now.minus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS).toEpochMilli();
        var to = now.plus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS).toEpochMilli();
        var result = analyticsRepository.searchRequestResponseTimes(
            queryContext,
            new RequestResponseTimeQueryCriteria(List.of(API_ID), from, to, EnumSet.of(V4))
        );

        // Verify that the result is null
        assertNotNull(result);
    }

    @Test
    public void testSearchRequestsCountByEvent() throws Exception {
        Assert.assertNotNull(analyticsRepository);

        var now = Instant.now();
        var from = now.minus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS);
        var to = now.plus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS);

        var result = analyticsRepository.searchRequestsCountByEvent(
            queryContext,
            new RequestsCountByEventQuery(new SearchTermId(SearchTermId.SearchTerm.API, API_ID), new TimeRange(from, to), Optional.empty())
        );
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
