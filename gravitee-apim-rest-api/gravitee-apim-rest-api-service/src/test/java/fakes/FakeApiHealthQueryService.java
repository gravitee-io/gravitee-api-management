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
package fakes;

import io.gravitee.apim.core.api_health.model.AvailabilityHealthCheck;
import io.gravitee.apim.core.api_health.model.AverageHealthCheckResponseTime;
import io.gravitee.apim.core.api_health.model.AverageHealthCheckResponseTimeOvertime;
import io.gravitee.apim.core.api_health.model.HealthCheckLog;
import io.gravitee.apim.core.api_health.query_service.ApiHealthQueryService;
import io.gravitee.common.data.domain.Page;
import io.reactivex.rxjava3.core.Maybe;

public class FakeApiHealthQueryService implements ApiHealthQueryService {

    public AverageHealthCheckResponseTime averageHealthCheckResponseTime;
    public AvailabilityHealthCheck availabilityHealthCheck;
    public AverageHealthCheckResponseTimeOvertime averageHealthCheckResponseTimeOvertime;
    public Page<HealthCheckLog> healthCheckLogs;

    @Override
    public Maybe<AverageHealthCheckResponseTime> averageResponseTime(ApiFieldPeriodQuery query) {
        return averageHealthCheckResponseTime == null ? Maybe.empty() : Maybe.just(averageHealthCheckResponseTime);
    }

    @Override
    public Maybe<AvailabilityHealthCheck> availability(ApiFieldPeriodQuery query) {
        return availabilityHealthCheck == null ? Maybe.empty() : Maybe.just(availabilityHealthCheck);
    }

    @Override
    public Maybe<AverageHealthCheckResponseTimeOvertime> averageResponseTimeOvertime(AverageHealthCheckResponseTimeOvertimeQuery query) {
        return averageHealthCheckResponseTimeOvertime == null ? Maybe.empty() : Maybe.just(averageHealthCheckResponseTimeOvertime);
    }

    @Override
    public Maybe<Page<HealthCheckLog>> searchLogs(SearchLogsQuery query) {
        return healthCheckLogs == null ? Maybe.empty() : Maybe.just(healthCheckLogs);
    }

    public void reset() {
        averageHealthCheckResponseTime = null;
        availabilityHealthCheck = null;
        averageHealthCheckResponseTimeOvertime = null;
        healthCheckLogs = null;
    }
}
