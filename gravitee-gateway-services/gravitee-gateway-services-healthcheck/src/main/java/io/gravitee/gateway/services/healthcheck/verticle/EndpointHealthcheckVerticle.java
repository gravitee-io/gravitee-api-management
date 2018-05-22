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
import io.gravitee.common.util.ChangeListener;
import io.gravitee.common.util.ObservableSet;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.definition.model.EndpointGroup;
import io.gravitee.definition.model.services.healthcheck.HealthCheckService;
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

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    private final Map<Api, List<EndpointRuleTrigger>> apiTimers = new HashMap<>();

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
        if (healthcheckEnabled(api)) {
            Set<Endpoint> endpoints = api.getProxy()
                    .getGroups()
                    .stream()
                    .flatMap(group -> group.getEndpoints().stream())
                    .collect(Collectors.toSet());

            LOGGER.info("Health-check for API id[{}] name[{}] is enabled", api.getId(), api.getName());
            apiTimers.put(api, new ArrayList<>());

            if (endpoints instanceof ObservableSet) {
                ((ObservableSet) endpoints).addListener(new EndpointsListener(api));
            }

            List<EndpointRule> healthcheckEndpoints = endpointResolver.resolve(api);
            if (!healthcheckEndpoints.isEmpty()) {
                healthcheckEndpoints.forEach(rule -> addTrigger(api, rule));
            }
        }
    }

    private boolean healthcheckEnabled(Api api) {
        HealthCheckService rootHealthCheck = api.getServices().get(HealthCheckService.class);
        return api.isEnabled() && (rootHealthCheck != null && rootHealthCheck.isEnabled());
    }

    private void stopHealthCheck(Api api) {
        removeTriggers(api);
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

    private void addTrigger(Api api, EndpointRule rule) {
        HttpEndpointRuleHandler runner = new HttpEndpointRuleHandler(vertx, rule);
        runner.setStatusHandler(statusReporter);

        long timerId = vertx.setPeriodic(getDelayMillis(rule.trigger()), runner);
        apiTimers.get(api).add(new EndpointRuleTrigger(timerId, rule.endpoint()));

        LOGGER.info("Add health-check trigger id[{}] for endpoint name[{}] target[{}] each rate[{}] unit[{}]",
                timerId,
                rule.endpoint().getName(), rule.endpoint().getTarget(),
                rule.trigger().getRate(), rule.trigger().getUnit());
    }

    private void removeTriggers(Api api) {
        List<EndpointRuleTrigger> triggers = apiTimers.remove(api);
        if (triggers != null) {
            LOGGER.info("Stop health-check for API id[{}] name[{}]", api.getId(), api.getName());
            triggers.forEach(trigger -> vertx.cancelTimer(trigger.getTimerId()));
        }
    }

    private void removeTrigger(Api api, Endpoint endpoint) {
        List<EndpointRuleTrigger> endpointRuleTriggers = apiTimers.get(api);
        if (endpointRuleTriggers != null) {
            Optional<EndpointRuleTrigger> endpointRuleTrigger = endpointRuleTriggers
                    .stream()
                    .filter(trigger -> trigger.getEndpoint().equals(endpoint)).findFirst();

            endpointRuleTrigger.ifPresent(trigger -> {
                LOGGER.info("Remove health-check trigger id[{}] for endpoint name[{}] type[{}] target[{}]",
                        trigger.getTimerId(),
                        endpoint.getName(), endpoint.getType(), endpoint.getTarget());
                vertx.cancelTimer(trigger.getTimerId());
                endpointRuleTriggers.remove(trigger);
            });
        }
    }

    private class EndpointsListener implements ChangeListener<Endpoint> {

        private final Api api;

        EndpointsListener(Api api) {
            this.api = api;
        }

        @Override
        public boolean preAdd(Endpoint endpoint) {
            return false;
        }

        @Override
        public boolean postAdd(Endpoint endpoint) {
            EndpointRule rule = endpointResolver.resolve(api, endpoint);
            if (rule != null) {
                addTrigger(api, rule);
            }
            return false;
        }

        @Override
        public boolean preRemove(Endpoint endpoint) {
            return false;
        }

        @Override
        public boolean postRemove(Endpoint endpoint) {
            removeTrigger(api, endpoint);
            return false;
        }
    }

    private class EndpointRuleTrigger {
        private final long timerId;
        private final Endpoint endpoint;

        EndpointRuleTrigger(long timerId, Endpoint endpoint) {
            this.timerId = timerId;
            this.endpoint = endpoint;
        }

        long getTimerId() {
            return timerId;
        }

        Endpoint getEndpoint() {
            return endpoint;
        }
    }
}
