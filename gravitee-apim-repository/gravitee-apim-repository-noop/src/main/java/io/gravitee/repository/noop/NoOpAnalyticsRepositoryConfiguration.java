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
package io.gravitee.repository.noop;

import io.gravitee.repository.analytics.api.AnalyticsRepository;
import io.gravitee.repository.healthcheck.api.HealthCheckRepository;
import io.gravitee.repository.log.api.LogRepository;
import io.gravitee.repository.monitoring.MonitoringRepository;
import io.gravitee.repository.noop.analytics.NoOpAnalyticsRepository;
import io.gravitee.repository.noop.healthcheck.NoOpHealthCheckRepository;
import io.gravitee.repository.noop.log.NoOpLogRepository;
import io.gravitee.repository.noop.monitor.NoOpMonitoringRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
public class NoOpAnalyticsRepositoryConfiguration {

    @Bean
    public AnalyticsRepository analyticsRepository() {
        return new NoOpAnalyticsRepository();
    }

    @Bean
    public HealthCheckRepository healthCheckRepository() {
        return new NoOpHealthCheckRepository();
    }

    @Bean
    public io.gravitee.repository.healthcheck.v4.api.HealthCheckRepository healthCheckRepositoryV4() {
        return new io.gravitee.repository.noop.healthcheck.v4.NoOpHealthCheckRepository();
    }

    @Bean
    public LogRepository logRepository() {
        return new NoOpLogRepository();
    }

    @Bean
    public io.gravitee.repository.log.v4.api.LogRepository logRepositoryV4() {
        return new io.gravitee.repository.noop.log.v4.NoOpLogRepository();
    }

    @Bean
    public MonitoringRepository monitoringRepository() {
        return new NoOpMonitoringRepository();
    }
}
