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
package io.gravitee.repository.elasticsearch.v4.healthcheck;

import io.gravitee.elasticsearch.utils.Type;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.elasticsearch.AbstractElasticsearchRepository;
import io.gravitee.repository.elasticsearch.configuration.RepositoryConfiguration;
import io.gravitee.repository.elasticsearch.utils.ClusterUtils;
import io.gravitee.repository.elasticsearch.v4.healthcheck.adapter.AverageHealthCheckResponseTimeAdapter;
import io.gravitee.repository.elasticsearch.v4.healthcheck.adapter.AverageHealthCheckResponseTimeOvertimeAdapter;
import io.gravitee.repository.healthcheck.v4.api.HealthCheckRepository;
import io.gravitee.repository.healthcheck.v4.model.AverageHealthCheckResponseTime;
import io.gravitee.repository.healthcheck.v4.model.AverageHealthCheckResponseTimeOvertime;
import io.gravitee.repository.healthcheck.v4.model.AverageHealthCheckResponseTimeOvertimeQuery;
import io.gravitee.repository.healthcheck.v4.model.AverageHealthCheckResponseTimeQuery;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HealthCheckElasticsearchRepository extends AbstractElasticsearchRepository implements HealthCheckRepository {

    private final String[] clusters;

    private final AverageHealthCheckResponseTimeAdapter averageHealthCheckResponseTimeAdapter = new AverageHealthCheckResponseTimeAdapter();
    private final AverageHealthCheckResponseTimeOvertimeAdapter averageHealthCheckResponseTimeOvertimeAdapter =
        new AverageHealthCheckResponseTimeOvertimeAdapter();

    public HealthCheckElasticsearchRepository(RepositoryConfiguration configuration) {
        clusters = ClusterUtils.extractClusterIndexPrefixes(configuration);
    }

    @Override
    public Optional<AverageHealthCheckResponseTime> averageResponseTime(
        QueryContext queryContext,
        AverageHealthCheckResponseTimeQuery query
    ) {
        var index = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.HEALTH_CHECK, clusters);

        return this.client.search(index, null, averageHealthCheckResponseTimeAdapter.adaptQuery(query))
            .map(averageHealthCheckResponseTimeAdapter::adaptResponse)
            .blockingGet();
    }

    @Override
    public Optional<AverageHealthCheckResponseTimeOvertime> averageResponseTimeOvertime(
        QueryContext queryContext,
        AverageHealthCheckResponseTimeOvertimeQuery query
    ) {
        var index = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.HEALTH_CHECK, clusters);

        return this.client.search(index, null, averageHealthCheckResponseTimeOvertimeAdapter.adaptQuery(query, info))
            .map(averageHealthCheckResponseTimeOvertimeAdapter::adaptResponse)
            .blockingGet();
    }
}
