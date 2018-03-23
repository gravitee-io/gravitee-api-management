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

import io.gravitee.repository.analytics.query.tabular.TabularResponse;
import io.gravitee.repository.elasticsearch.log.ElasticLogRepository;
import io.gravitee.repository.log.model.ExtendedLog;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static io.gravitee.repository.analytics.query.DateRangeBuilder.lastDays;
import static io.gravitee.repository.analytics.query.IntervalBuilder.hours;
import static io.gravitee.repository.analytics.query.QueryBuilders.tabular;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ElasticsearchLogRepositoryTest extends AbstractElasticsearchRepositoryTest {

    @Autowired
    private ElasticLogRepository logRepository;

    @Test
    public void testFindById() throws Exception {
        ExtendedLog log = logRepository.findById("29381bce-df59-47b2-b81b-cedf59c7b23e");

        Assert.assertNotNull(log);
    }

    @Test
    public void testTabular_withQuery() throws Exception {
        TabularResponse response = logRepository.query(
                tabular()
                        .timeRange(lastDays(60), hours(1))
                        .query("api:be0aa9c9-ca1c-4d0a-8aa9-c9ca1c5d0aab")
                        .page(1)
                        .size(20)
                        .build());

        Assert.assertNotNull(response);
    }

    @Test
    public void testTabular_withoutQuery() throws Exception {
        TabularResponse response = logRepository.query(
                tabular()
                        .timeRange(lastDays(60), hours(1))
                        .page(1)
                        .size(20)
                        .build());

        Assert.assertNotNull(response);
    }
}
