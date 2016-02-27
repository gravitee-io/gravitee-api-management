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
package io.gravitee.gateway.services.monitoring;

import io.gravitee.gateway.core.reporter.ReporterService;
import io.gravitee.gateway.services.monitoring.metrics.JvmMetricsService;
import io.gravitee.reporter.api.monitor.Monitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
public class MonitorThread implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonitorThread.class);

    @Autowired
    private JvmMetricsService jvmMetricsService;

    @Autowired
    private ReporterService reporterService;

    @Override
    public void run() {
        LOGGER.debug("Run monitor tick for gateway");

        Monitor monitor = Monitor.on(System.currentTimeMillis()).build();

        monitor.setCpuUsage(jvmMetricsService.getCpuUsage());
        monitor.setThreadsState(jvmMetricsService.getThreadStates());
        monitor.setGarbageCollector(jvmMetricsService.getGarbageCollector());
        monitor.setMemoryUsage(jvmMetricsService.getMemoryUsage());

        reporterService.report(monitor);
    }
}