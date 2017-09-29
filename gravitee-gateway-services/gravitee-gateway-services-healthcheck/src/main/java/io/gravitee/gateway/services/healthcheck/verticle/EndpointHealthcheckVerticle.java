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
package io.gravitee.gateway.services.healthcheck.verticle;

import io.gravitee.common.event.Event;
import io.gravitee.common.event.EventListener;
import io.gravitee.common.event.EventManager;
import io.gravitee.definition.model.services.schedule.Trigger;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.gravitee.gateway.services.healthcheck.EndpointHealthcheckResolver;
import io.gravitee.gateway.services.healthcheck.EndpointRule;
import io.gravitee.gateway.services.healthcheck.http.HttpEndpointRuleHandler;
import io.gravitee.gateway.services.healthcheck.reporter.StatusReporter;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EndpointHealthcheckVerticle extends AbstractVerticle implements EventListener<ReactorEvent, Reactable> {

    private final static Logger LOGGER = LoggerFactory.getLogger(EndpointHealthcheckVerticle.class);

    @Autowired
    private EventManager eventManager;

    @Autowired
    private StatusReporter statusReporter;

    @Autowired
    private EndpointHealthcheckResolver endpointResolver;

    private final Map<Api, List<Long>> apiTimers = new HashMap<>();

    @Override
    public void start(final Future<Void> startedResult) {
        eventManager.subscribeForEvents(this, ReactorEvent.class);
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
            List<Long> timers = new ArrayList<>();

            if (! healthcheckEndpoints.isEmpty()) {
                LOGGER.info("Start health-check for API {} [{}]", api.getId(), api.getName());

                healthcheckEndpoints.forEach(rule -> {
                    LOGGER.info("Add a trigger to check health status for endpoint {} [{}] each {} {} ",
                            rule.endpoint().getName(), rule.endpoint().getTarget(),
                            rule.trigger().getRate(),
                            rule.trigger().getUnit());

                    HttpEndpointRuleHandler runner = new HttpEndpointRuleHandler(vertx, rule);
                    runner.setStatusHandler(statusReporter);

                    long periodic = vertx.setPeriodic(getDelayMillis(rule.trigger()), runner);
                    timers.add(periodic);
                });

                apiTimers.put(api, timers);
            }
        }
    }

    private void stopHealthCheck(Api api) {
        List<Long> timers = apiTimers.remove(api);
        if (timers != null) {
            LOGGER.info("Stop health-check for API {} [{}]", api.getId(), api.getName());
            timers.forEach(timerId -> vertx.cancelTimer(timerId));
        }
    }

    private long getDelayMillis(Trigger trigger) {
        switch (trigger.getUnit()) {
            case MILLISECONDS:
                return trigger.getRate();
            case SECONDS:
                return trigger.getRate() * 1000;
            case MINUTES:
                return trigger.getRate() * 1000 * 60;
            case HOURS:
                return trigger.getRate() * 1000 * 60 * 60;
        }

        return -1;
    }
}
