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
import io.gravitee.definition.model.services.schedule.Trigger;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.gravitee.gateway.services.healthcheck.EndpointHealthcheckResolver;
import io.gravitee.gateway.services.healthcheck.EndpointRule;
import io.gravitee.gateway.services.healthcheck.http.HttpEndpointRuleHandler;
import io.gravitee.gateway.services.healthcheck.reporter.StatusReporter;
import io.gravitee.gateway.services.healthcheck.rule.EndpointRuleHandler;
import io.gravitee.node.api.Node;
import io.gravitee.plugin.alert.AlertEventProducer;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
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

    @Autowired
    private AlertEventProducer alertEventProducer;

    @Autowired
    private Node node;

    @Autowired
    private Environment environment;

    private final Map<Api, List<EndpointRuleTrigger>> apiTimers = new ConcurrentHashMap<>();

    @Override
    public void start(final Future<Void> startedResult) {
        eventManager.subscribeForEvents(this, ReactorEvent.class);
    }

    @Override
    public void onEvent(Event<ReactorEvent, Reactable> event) {
        final Api api = (Api) event.content();

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
        api.getProxy().getGroups()
                .stream()
                .filter(group -> group.getEndpoints() != null)
                .forEach(group -> {
                    final Set<Endpoint> endpoints = group.getEndpoints();
                    if (endpoints instanceof ObservableSet) {
                        apiTimers.put(api, new ArrayList<>());
                        ((ObservableSet) endpoints).addListener(new EndpointsListener(api));
                    }
                });

        // Configure triggers on resolved API endpoints
        final List<EndpointRule> healthCheckEndpoints = endpointResolver.resolve(api);
        if (!healthCheckEndpoints.isEmpty()) {
            LOGGER.debug("Health-check for API id[{}] name[{}] is enabled", api.getId(), api.getName());
            apiTimers.put(api, new ArrayList<>());
            healthCheckEndpoints.forEach(rule -> addTrigger(api, rule));
        }
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
            case DAYS:
                return trigger.getRate() * 1000 * 60 * 60 * 24;
        }

        return -1;
    }

    private void addTrigger(Api api, EndpointRule rule) {
        try {
            EndpointRuleHandler runner = rule.createRunner(vertx, rule, environment);
            runner.setStatusHandler(statusReporter);
            runner.setAlertEventProducer(alertEventProducer);
            runner.setNode(node);

            long timerId = vertx.setPeriodic(getDelayMillis(rule.trigger()), runner);
            apiTimers.get(api).add(new EndpointRuleTrigger(timerId, runner, rule.endpoint()));

            LOGGER.debug("Add health-check trigger id[{}] for endpoint name[{}] target[{}] each rate[{}] unit[{}]",
                    timerId,
                    rule.endpoint().getName(), rule.endpoint().getTarget(),
                    rule.trigger().getRate(), rule.trigger().getUnit());
        } catch (Exception ex) {
            LOGGER.error("An error occurs while creating an health-check runner", ex);
        }
    }

    private void removeTriggers(Api api) {
        List<EndpointRuleTrigger> triggers = apiTimers.remove(api);
        if (triggers != null) {
            LOGGER.debug("Stop health-check for API id[{}] name[{}]", api.getId(), api.getName());
            triggers.forEach(trigger -> {
                vertx.cancelTimer(trigger.getTimerId());
                trigger.getEndpointRuleHandler().close();
            });
        }
    }

    private void removeTrigger(Api api, Endpoint endpoint) {
        List<EndpointRuleTrigger> endpointRuleTriggers = apiTimers.get(api);
        if (endpointRuleTriggers != null) {
            Optional<EndpointRuleTrigger> endpointRuleTrigger = endpointRuleTriggers
                    .stream()
                    .filter(trigger -> trigger.getEndpoint().equals(endpoint)).findFirst();

            endpointRuleTrigger.ifPresent(trigger -> {
                LOGGER.debug("Remove health-check trigger id[{}] for endpoint name[{}] type[{}] target[{}]",
                        trigger.getTimerId(),
                        endpoint.getName(), endpoint.getType(), endpoint.getTarget());
                vertx.cancelTimer(trigger.getTimerId());
                trigger.getEndpointRuleHandler().close();
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
        private final EndpointRuleHandler ruleHandler;
        private final Endpoint endpoint;

        EndpointRuleTrigger(long timerId, EndpointRuleHandler ruleHandler, Endpoint endpoint) {
            this.timerId = timerId;
            this.ruleHandler = ruleHandler;
            this.endpoint = endpoint;
        }

        long getTimerId() {
            return timerId;
        }

        public EndpointRuleHandler getEndpointRuleHandler() {
            return ruleHandler;
        }

        Endpoint getEndpoint() {
            return endpoint;
        }
    }
}
