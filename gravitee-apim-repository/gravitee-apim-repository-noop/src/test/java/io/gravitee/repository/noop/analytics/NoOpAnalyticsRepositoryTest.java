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
package io.gravitee.repository.noop.analytics;

import static io.gravitee.repository.analytics.query.DateRangeBuilder.lastDays;
import static io.gravitee.repository.analytics.query.IntervalBuilder.hours;
import static io.gravitee.repository.analytics.query.QueryBuilders.dateHistogram;

import io.gravitee.repository.analytics.api.AnalyticsRepository;
import io.gravitee.repository.analytics.query.response.histogram.DateHistogramResponse;
import io.gravitee.repository.noop.AbstractNoOpRepositoryTest;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NoOpAnalyticsRepositoryTest extends AbstractNoOpRepositoryTest {

    @Autowired
    private AnalyticsRepository analyticsRepository;

    @Test
    public void testQuery() throws Exception {
        Assert.assertNotNull(analyticsRepository);

        DateHistogramResponse response = analyticsRepository.query(dateHistogram().timeRange(lastDays(30), hours(1)).build());

        Assert.assertNull(response);
    }
}
