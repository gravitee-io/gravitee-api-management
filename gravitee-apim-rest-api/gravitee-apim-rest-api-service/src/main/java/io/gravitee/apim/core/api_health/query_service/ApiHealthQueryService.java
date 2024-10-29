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
package io.gravitee.apim.core.api_health.query_service;

import io.gravitee.apim.core.api_health.model.AvailabilityHealthCheck;
import io.gravitee.apim.core.api_health.model.AverageHealthCheckResponseTime;
import io.gravitee.apim.core.api_health.model.AverageHealthCheckResponseTimeOvertime;
import io.reactivex.rxjava3.core.Maybe;
import java.time.Duration;
import java.time.Instant;
import lombok.Builder;

public interface ApiHealthQueryService {
    Maybe<AverageHealthCheckResponseTime> averageResponseTime(ApiFieldPeriodQuery query);
    Maybe<AvailabilityHealthCheck> availability(ApiFieldPeriodQuery query);
    Maybe<AverageHealthCheckResponseTimeOvertime> averageResponseTimeOvertime(AverageHealthCheckResponseTimeOvertimeQuery query);

    @Builder(toBuilder = true)
    record ApiFieldPeriodQuery(String organizationId, String environmentId, String apiId, String field, Instant from, Instant to) {}

    @Builder(toBuilder = true)
    record AverageHealthCheckResponseTimeOvertimeQuery(
        String organizationId,
        String environmentId,
        String apiId,
        Instant from,
        Instant to,
        Duration interval
    ) {}
}
