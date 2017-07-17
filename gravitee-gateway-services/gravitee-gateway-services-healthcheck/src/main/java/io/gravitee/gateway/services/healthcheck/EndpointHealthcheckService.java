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
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.gateway.services.healthcheck.http.HttpEndpointRuleRunner;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EndpointHealthcheckService extends AbstractService implements EventListener<ReactorEvent, Reactable> {

    private final static Logger LOGGER = LoggerFactory.getLogger(EndpointHealthcheckService.class);

    @Value("${services.healthcheck.threads:3}")
    private int threads;

    @Autowired
    private EventManager eventManager;

    @Autowired
    private ReporterService reporterService;

    @Autowired
    private EndpointHealthcheckResolver endpointResolver;

    private ExecutorService executorService;

    private final Map<Api, List<EndpointHealthcheckFuture>> scheduledTasks = new HashMap<>();

    @Autowired
    private Vertx vertx;

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        eventManager.subscribeForEvents(this, ReactorEvent.class);

        executorService = Executors.newScheduledThreadPool(threads, new ThreadFactory() {
            private int counter = 0;
            private String prefix = "endpoint-healthcheck";

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
            List<EndpointRule> healthcheckEndpoints = endpointResolver.resolve(api);
            List<EndpointHealthcheckFuture> runners = new ArrayList<>();

            if (! healthcheckEndpoints.isEmpty()) {
                LOGGER.info("Start health-check for API {} [{}]", api.getId(), api.getName());

                healthcheckEndpoints.forEach(rule -> {
                    LOGGER.info("Add a trigger to check health status for endpoint {} [{}] each {} {} ",
                            rule.endpoint().getName(), rule.endpoint().getTarget(),
                            rule.trigger().getRate(),
                            rule.trigger().getUnit());

                    HttpEndpointRuleRunner runner = new HttpEndpointRuleRunner(vertx, rule);
                    runner.setReporterService(reporterService);

                    EndpointHealthcheckFuture endpointFuture = new EndpointHealthcheckFuture(
                            rule.endpoint(),
                            ((ScheduledExecutorService) executorService).scheduleWithFixedDelay(
                                    runner, 0,
                                    rule.trigger().getRate(),
                                    rule.trigger().getUnit())
                    );

                    runners.add(endpointFuture);
                });

                scheduledTasks.put(api, runners);
            }
        }
    }

    private void stopHealthCheck(Api api) {
        List<EndpointHealthcheckFuture> futures = scheduledTasks.remove(api);
        if (futures != null) {
            LOGGER.info("Stop health-check for API {} [{}]", api.getId(), api.getName());

            futures.forEach(endpointFuture -> {
                if (! endpointFuture.isCancelled()) {
                    LOGGER.info("Stop health-check trigger for endpoint {} [{}]",
                            endpointFuture.getEndpoint().getName(), endpointFuture.getEndpoint().getTarget());
                    endpointFuture.cancel(true);
                }
            });
        }
    }
}
