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
package io.gravitee.gateway.report.spring;

import io.gravitee.gateway.report.ReporterService;
import io.gravitee.gateway.report.guard.LogGuardService;
import io.gravitee.gateway.report.impl.NodeMonitoringReporterService;
import io.gravitee.gateway.report.impl.ReporterServiceImpl;
import io.gravitee.node.monitoring.healthcheck.NodeHealthCheckService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
public class ReporterConfiguration {

    @Bean
    public ReporterService reporterService() {
        return new ReporterServiceImpl();
    }

    @Bean
    public NodeMonitoringReporterService nodeMonitoringReporterService() {
        return new NodeMonitoringReporterService();
    }

    @Bean
    public LogGuardService logGuardService(
        @Value("${reporters.logging.memory_pressure_guard.enabled:true}") boolean enabled,
        @Value("${reporters.logging.memory_pressure_guard.strategy.type:cooldown}") String strategy,
        @Value("${reporters.logging.memory_pressure_guard.strategy.cooldown.duration:60}") int cooldownDurationInSeconds,
        NodeHealthCheckService nodeHealthCheckService
    ) {
        return new LogGuardService(enabled, strategy, cooldownDurationInSeconds, nodeHealthCheckService);
    }
}
