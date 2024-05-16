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
package io.gravitee.repository.noop.healthcheck;

import static io.gravitee.repository.healthcheck.query.QueryBuilders.dateHistogram;

import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.healthcheck.api.HealthCheckRepository;
import io.gravitee.repository.healthcheck.query.log.ExtendedLog;
import io.gravitee.repository.healthcheck.query.response.histogram.DateHistogramResponse;
import io.gravitee.repository.noop.AbstractNoOpRepositoryTest;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Teaom
 */
public class NoOpHealthCheckRepositoryTest extends AbstractNoOpRepositoryTest {

    private final QueryContext queryContext = new QueryContext("org#1", "env#1");

    @Autowired
    private HealthCheckRepository healthCheckRepository;

    @Test
    public void testQuery() throws AnalyticsException {
        Assert.assertNotNull(healthCheckRepository);

        DateHistogramResponse response = healthCheckRepository.query(queryContext, dateHistogram().query("any_query").build());

        Assert.assertNull(response);
    }

    @Test
    public void testFindById() throws AnalyticsException {
        Assert.assertNotNull(healthCheckRepository);

        ExtendedLog response = healthCheckRepository.findById(queryContext, "any_id");

        Assert.assertNull(response);
    }
}
