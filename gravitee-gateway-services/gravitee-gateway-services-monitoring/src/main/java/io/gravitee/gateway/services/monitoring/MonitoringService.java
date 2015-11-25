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

import io.gravitee.common.event.Event;
import io.gravitee.common.event.EventListener;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.service.AbstractService;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.Monitoring;
import io.gravitee.gateway.core.event.ApiEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class MonitoringService extends AbstractService implements EventListener<ApiEvent, Api> {

    private final static Logger LOGGER = LoggerFactory.getLogger(MonitoringService.class);

    @Autowired
    private EventManager eventManager;

    private final Map<Api, ExecutorService> monitors = new HashMap<>();

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        eventManager.subscribeForEvents(this, ApiEvent.class);
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        monitors.forEach((api, executorService) -> stopMonitor(api));
    }

    @Override
    protected String name() {
        return "Endpoint Monitoring Service";
    }

    @Override
    public void onEvent(Event<ApiEvent, Api> event) {
        final Api api = event.content();

        switch (event.type()) {
            case DEPLOY:
                startMonitor(api);
                break;
            case UNDEPLOY:
                stopMonitor(api);
                break;
            case UPDATE:
        }
        if (api.getMonitoring() != null && api.getMonitoring().isEnabled()) {
            startMonitor(api);
        }
    }

    private void startMonitor(Api api) {
        Monitoring monitoring = api.getMonitoring();
        if (monitoring != null) {
            if (monitoring.isEnabled()) {
                LOGGER.info("Create an executor to monitor {}", api);

                EndpointMonitor monitor = new EndpointMonitor(api);

                ExecutorService executor = Executors.newSingleThreadScheduledExecutor(
                        r -> new Thread(r, "monitor-" + api.getName()));

                monitors.put(api, executor);

                LOGGER.info("Start monitor for {}", api);
                ((ScheduledExecutorService) executor).scheduleWithFixedDelay(monitor, 0, 5, TimeUnit.SECONDS);
            } else {
                LOGGER.info("Monitoring is disabled for {}", api);
            }
        }
    }

    private void stopMonitor(Api api) {
        ExecutorService executor = monitors.remove(api);
        if (executor != null) {
            if (! executor.isShutdown()) {
                LOGGER.info("Stop monitor for {}", api);
                executor.shutdownNow();
            } else {
                LOGGER.info("Monitor already shutdown for {}", api);
            }
        }
    }
}
