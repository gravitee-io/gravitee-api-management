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
package io.gravitee.apim.infra.query_service.api_health;

import io.gravitee.apim.core.api_health.model.AverageHealthCheckResponseTime;
import io.gravitee.apim.core.api_health.model.AverageHealthCheckResponseTimeOvertime;
import io.gravitee.apim.core.api_health.query_service.ApiHealthQueryService;
import io.gravitee.apim.infra.adapter.ApiHealthAdapter;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.healthcheck.v4.api.HealthCheckRepository;
import java.util.ArrayList;
import java.util.Optional;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class ApiHealthQueryServiceImpl implements ApiHealthQueryService {

    private final HealthCheckRepository healthCheckRepository;

    public ApiHealthQueryServiceImpl(@Lazy HealthCheckRepository healthCheckRepository) {
        this.healthCheckRepository = healthCheckRepository;
    }

    @Override
    public Optional<AverageHealthCheckResponseTime> averageResponseTime(AverageHealthCheckResponseTimeQuery query) {
        return healthCheckRepository
            .averageResponseTime(new QueryContext(query.organizationId(), query.environmentId()), ApiHealthAdapter.INSTANCE.map(query))
            .map(ApiHealthAdapter.INSTANCE::map);
    }

    @Override
    public Optional<AverageHealthCheckResponseTimeOvertime> averageResponseTimeOvertime(AverageHealthCheckResponseTimeOvertimeQuery query) {
        return healthCheckRepository
            .averageResponseTimeOvertime(
                new QueryContext(query.organizationId(), query.environmentId()),
                ApiHealthAdapter.INSTANCE.map(query)
            )
            .map(result ->
                new AverageHealthCheckResponseTimeOvertime(
                    new AverageHealthCheckResponseTimeOvertime.TimeRange(query.from(), query.to(), query.interval()),
                    new ArrayList<>(result.buckets().values())
                )
            );
    }
}
