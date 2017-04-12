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
package io.gravitee.gateway.services.healthcheck;

import io.gravitee.common.event.Event;
import io.gravitee.common.event.EventListener;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.service.AbstractService;
import io.gravitee.definition.model.services.healthcheck.HealthCheck;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.gravitee.gateway.report.ReporterService;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public class HealthCheckService extends AbstractService implements EventListener<ReactorEvent, Reactable> {

    private final static Logger LOGGER = LoggerFactory.getLogger(HealthCheckService.class);

    @Value("${services.healthcheck.threads:3}")
    private int threads;

    @Autowired
    private EventManager eventManager;

    @Autowired
    private ReporterService reporterService;

    private ExecutorService executorService;

    private final Map<Api, ScheduledFuture> scheduledTasks = new HashMap<>();

    private final Vertx vertx = Vertx.vertx();

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        eventManager.subscribeForEvents(this, ReactorEvent.class);

        executorService = Executors.newScheduledThreadPool(threads, new ThreadFactory() {
            private int counter = 0;
            private String prefix = "healthcheck";

            public Thread newThread(Runnable r) {
                return new Thread(r, prefix + '-' + counter++);
            }
        });
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        if (executorService != null) {
            executorService.shutdown();
        }

        vertx.close(event -> LOGGER.info("Health-check HTTP client has been closed."));
    }

    @Override
    protected String name() {
        return "Health-check service";
    }

    @Override
    public void onEvent(Event<ReactorEvent, Reactable> event) {
        final Api api = (Api) event.content().item();

        switch (event.type()) {
            case DEPLOY:
                startHealthCheck(api);
                break;
            case UNDEPLOY:
                stopHealthCheck(api);
                break;
            case UPDATE:
                stopHealthCheck(api);
                startHealthCheck(api);
                break;
        }
    }

    private void startHealthCheck(Api api) {
        if (api.isEnabled()) {
            HealthCheck healthCheck = api.getServices().get(HealthCheck.class);
            if (healthCheck != null && healthCheck.isEnabled()) {
                EndpointHealthCheck monitor = new EndpointHealthCheck(vertx, api);
                monitor.setReporterService(reporterService);

                LOGGER.info("Add a scheduled task to health-check endpoints each {} {} ", healthCheck.getInterval(), healthCheck.getUnit());
                ScheduledFuture scheduledFuture = ((ScheduledExecutorService) executorService).scheduleWithFixedDelay(
                        monitor, 0, healthCheck.getInterval(), healthCheck.getUnit());

                scheduledTasks.put(api, scheduledFuture);
            } else {
                LOGGER.info("Health-check is disabled");
            }
        }
    }

    private void stopHealthCheck(Api api) {
        ScheduledFuture scheduledFuture = scheduledTasks.remove(api);
        if (scheduledFuture != null) {
            if (! scheduledFuture.isCancelled()) {
                LOGGER.info("Stop health-check");
                scheduledFuture.cancel(true);
            } else {
                LOGGER.info("Health-check already shutdown");
            }
        }
    }
}
