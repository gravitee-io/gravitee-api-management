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
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.gravitee.gateway.services.healthcheck.EndpointHealthcheckResolver;
import io.gravitee.gateway.services.healthcheck.EndpointRule;
import io.gravitee.gateway.services.healthcheck.reporter.StatusReporter;
import io.gravitee.gateway.services.healthcheck.rule.EndpointRuleCronHandler;
import io.gravitee.gateway.services.healthcheck.rule.EndpointRuleHandler;
import io.gravitee.node.api.Node;
import io.gravitee.plugin.alert.AlertEventProducer;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

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

    private final Map<Api, List<EndpointRuleCronHandler>> apiHandlers = new HashMap<>();

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
                    apiHandlers.put(api, new ArrayList<>());
                    ((ObservableSet) endpoints).addListener(new EndpointsListener(api));
                }
            });

        // Configure triggers on resolved API endpoints
        final List<EndpointRule> healthCheckEndpoints = endpointResolver.resolve(api);
        if (!healthCheckEndpoints.isEmpty()) {
            LOGGER.info("Health-check for API id[{}] name[{}] is enabled", api.getId(), api.getName());
            apiHandlers.put(api, new ArrayList<>());
            healthCheckEndpoints.forEach(rule -> addTrigger(api, rule));
        }
    }

    private void stopHealthCheck(Api api) {
        removeTriggers(api);
    }

    private void addTrigger(Api api, EndpointRule rule) {
        EndpointRuleHandler runner = rule.createRunner(vertx, rule);
        runner.setStatusHandler(statusReporter);
        runner.setAlertEventProducer(alertEventProducer);
        runner.setNode(node);

        EndpointRuleCronHandler cronHandler = new EndpointRuleCronHandler(vertx, rule);
        cronHandler.schedule(runner);

        apiHandlers.get(api).add(cronHandler);

        LOGGER.info("Add health-check for endpoint name[{}] target[{}] with cron[{}]",
            rule.endpoint().getName(),
            rule.endpoint().getTarget(),
            rule.schedule());
    }

    private void removeTriggers(Api api) {
        List<EndpointRuleCronHandler> triggers = apiHandlers.remove(api);
        if (triggers != null) {
            LOGGER.info("Stop health-check for API id[{}] name[{}]", api.getId(), api.getName());
            triggers.forEach(trigger -> trigger.cancel());
        }
    }

    private void removeTrigger(Api api, Endpoint endpoint) {
        List<EndpointRuleCronHandler> cronHandlers = apiHandlers.get(api);
        if (cronHandlers != null) {

            Optional<EndpointRuleCronHandler> endpointCronHandler = cronHandlers
                .stream()
                .filter(trigger -> trigger.getEndpoint().equals(endpoint)).findFirst();

            endpointCronHandler.ifPresent(trigger -> {
                LOGGER.info("Remove health-check trigger id[{}] for endpoint name[{}] type[{}] target[{}]",
                    trigger.getTimerId(),
                    endpoint.getName(), endpoint.getType(), endpoint.getTarget());
                trigger.cancel();
                cronHandlers.remove(trigger);
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

}
