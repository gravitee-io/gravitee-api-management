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
package io.gravitee.management.repository.proxy;

import io.gravitee.common.data.domain.Order;
import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.analytics.api.AnalyticsRepository;
import io.gravitee.repository.analytics.query.Query;
import io.gravitee.repository.analytics.query.response.HealthResponse;
import io.gravitee.repository.analytics.query.response.HitsResponse;
import io.gravitee.repository.analytics.query.response.Response;
import io.gravitee.repository.analytics.query.response.TopHitsResponse;
import io.gravitee.repository.analytics.query.response.histogram.HistogramResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
@Component
public class AnalyticsRepositoryProxy extends AbstractProxy<AnalyticsRepository> implements AnalyticsRepository {

    @Override
    public <T extends Response> T query(Query<T> query) throws AnalyticsException {
        return target.query(query);
    }

    @Override
    public HealthResponse query(String s, long l, long l1, long l2) throws AnalyticsException {
        return target.query(s, l, l1, l2);
    }

    @Override
    public HitsResponse query(String s, String s1, long l, long l1) throws AnalyticsException {
        return target.query(s, s1, l, l1);
    }

    @Override
    public TopHitsResponse query(String s, String s1, String s2, Order o, long l, long l1, int i) throws AnalyticsException {
        return target.query(s, s1, s2, o, l, l1, i);
    }

    @Override
    public HistogramResponse query(String s, String s1, String s2, List<String> s3, long l, long l1, long l2) throws AnalyticsException {
        return target.query(s, s1, s2, s3, l, l1, l2);
    }
}
