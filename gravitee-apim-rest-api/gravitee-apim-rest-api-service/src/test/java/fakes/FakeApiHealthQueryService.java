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

import io.gravitee.apim.core.api_health.model.AverageHealthCheckResponseTime;
import io.gravitee.apim.core.api_health.model.AverageHealthCheckResponseTimeOvertime;
import io.gravitee.apim.core.api_health.query_service.ApiHealthQueryService;
import java.util.Optional;

public class FakeApiHealthQueryService implements ApiHealthQueryService {

    public AverageHealthCheckResponseTime averageHealthCheckResponseTime;
    public AverageHealthCheckResponseTimeOvertime averageHealthCheckResponseTimeOvertime;

    @Override
    public Optional<AverageHealthCheckResponseTime> averageResponseTime(AverageHealthCheckResponseTimeQuery query) {
        return Optional.of(averageHealthCheckResponseTime);
    }

    @Override
    public Optional<AverageHealthCheckResponseTimeOvertime> averageResponseTimeOvertime(AverageHealthCheckResponseTimeOvertimeQuery query) {
        return Optional.of(averageHealthCheckResponseTimeOvertime);
    }

    public void reset() {
        averageHealthCheckResponseTime = null;
        averageHealthCheckResponseTimeOvertime = null;
    }
}
