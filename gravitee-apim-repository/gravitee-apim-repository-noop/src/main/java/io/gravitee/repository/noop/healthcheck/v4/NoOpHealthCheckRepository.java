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
package io.gravitee.repository.noop.healthcheck.v4;

import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.healthcheck.v4.api.HealthCheckRepository;
import io.gravitee.repository.healthcheck.v4.model.AverageHealthCheckResponseTime;
import io.gravitee.repository.healthcheck.v4.model.AverageHealthCheckResponseTimeOvertime;
import io.gravitee.repository.healthcheck.v4.model.AverageHealthCheckResponseTimeOvertimeQuery;
import io.gravitee.repository.healthcheck.v4.model.AverageHealthCheckResponseTimeQuery;
import java.util.Optional;

public class NoOpHealthCheckRepository implements HealthCheckRepository {

    @Override
    public Optional<AverageHealthCheckResponseTime> averageResponseTime(
        QueryContext queryContext,
        AverageHealthCheckResponseTimeQuery query
    ) {
        return Optional.empty();
    }

    @Override
    public Optional<AverageHealthCheckResponseTimeOvertime> averageResponseTimeOvertime(
        QueryContext queryContext,
        AverageHealthCheckResponseTimeOvertimeQuery query
    ) {
        return Optional.empty();
    }
}
