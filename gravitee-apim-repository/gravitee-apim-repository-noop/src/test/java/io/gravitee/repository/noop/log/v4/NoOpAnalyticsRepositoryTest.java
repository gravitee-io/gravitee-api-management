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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.log.v4.api.AnalyticsRepository;
import io.gravitee.repository.log.v4.model.analytics.AverageConnectionDurationQuery;
import io.gravitee.repository.log.v4.model.analytics.AverageMessagesPerRequestQuery;
import io.gravitee.repository.log.v4.model.analytics.RequestsCountQuery;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusRangesQuery;
import io.gravitee.repository.noop.AbstractNoOpRepositoryTest;
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

        var result = analyticsRepository.searchRequestsCount(queryContext, RequestsCountQuery.builder().apiId(API_ID).build());

        assertNotNull(result);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testSearchAverageMessagesPerRequest() {
        var result = analyticsRepository.searchAverageMessagesPerRequest(
            queryContext,
            AverageMessagesPerRequestQuery.builder().apiId(API_ID).build()
        );

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSearchAverageConnectionDuration() {
        var result = analyticsRepository.searchAverageConnectionDuration(
            queryContext,
            AverageConnectionDurationQuery.builder().apiId(API_ID).build()
        );
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSearchResponseStatusRanges() {
        var result = analyticsRepository.searchResponseStatusRanges(
            queryContext,
            ResponseStatusRangesQuery.builder().apiId(API_ID).build()
        );
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
