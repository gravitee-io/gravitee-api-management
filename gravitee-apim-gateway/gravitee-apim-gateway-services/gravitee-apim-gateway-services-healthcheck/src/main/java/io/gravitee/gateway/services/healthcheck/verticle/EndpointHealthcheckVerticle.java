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
import io.gravitee.gateway.services.healthcheck.context.ApiTemplateVariableProvider;
import io.gravitee.gateway.services.healthcheck.context.HealthCheckContext;
import io.gravitee.gateway.services.healthcheck.context.HealthCheckContextFactory;
import io.gravitee.gateway.services.healthcheck.reporter.StatusReporter;
import io.gravitee.gateway.services.healthcheck.rule.EndpointRuleCronHandler;
import io.gravitee.gateway.services.healthcheck.rule.EndpointRuleHandler;
import io.gravitee.node.api.Node;
import io.gravitee.plugin.alert.AlertEventProducer;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EndpointHealthcheckVerticle extends AbstractVerticle implements EventListener<ReactorEvent, Reactable> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointHealthcheckVerticle.class);

    @Autowired
    private EventManager eventManager;

    @Autowired
    private StatusReporter statusReporter;

    @Autowired
    private EndpointHealthcheckResolver endpointResolver;

    @Autowired
    private AlertEventProducer alertEventProducer;

    @Autowired
    private HealthCheckContextFactory healthCheckContextFactory;

    @Autowired
    private Node node;

    @Autowired
    private Environment environment;

    private final Map<Api, List<EndpointRuleCronHandler>> apiHandlers = new ConcurrentHashMap<>();

    @Override
    public void start(final Promise<Void> startPromise) {
        eventManager.subscribeForEvents(this, ReactorEvent.class);
        startPromise.complete();
    }

    @Override
    public void onEvent(Event<ReactorEvent, Reactable> event) {
        switch (event.type()) {
            case DEPLOY:
                startHealthCheck((Api) event.content());
                break;
            case UNDEPLOY:
                stopHealthCheck((Api) event.content());
                break;
            case UPDATE:
                stopHealthCheck((Api) event.content());
                startHealthCheck((Api) event.content());
                break;
        }
    }

    private void startHealthCheck(Api api) {
        api
            .getDefinition()
            .getProxy()
            .getGroups()
            .stream()
            .filter(group -> group.getEndpoints() != null)
            .forEach(
                group -> {
                    final Set<Endpoint> endpoints = group.getEndpoints();
                    if (endpoints instanceof ObservableSet) {
                        apiHandlers.put(api, new ArrayList<>());
                        ((ObservableSet) endpoints).addListener(new EndpointsListener(api));
                    }
                }
            );

        // Configure handlers on resolved API endpoints
        final List<EndpointRule> healthCheckEndpoints = endpointResolver.resolve(api.getDefinition());
        if (!healthCheckEndpoints.isEmpty()) {
            LOGGER.debug("Health-check for API id[{}] name[{}] is enabled", api.getId(), api.getName());
            apiHandlers.put(api, new ArrayList<>());
            healthCheckEndpoints.forEach(rule -> addHandler(api, rule));
        }
    }

    private void stopHealthCheck(Api api) {
        removeHandlers(api);
    }

    private void addHandler(Api api, EndpointRule rule) {
        try {
            final HealthCheckContext healthCheckContext = healthCheckContextFactory.create(new ApiTemplateVariableProvider(api));

            EndpointRuleHandler runner = rule.createRunner(vertx, rule, healthCheckContext.getTemplateEngine(), environment);
            runner.setStatusHandler(statusReporter);
            runner.setAlertEventProducer(alertEventProducer);
            runner.setNode(node);
            EndpointRuleCronHandler cronHandler = new EndpointRuleCronHandler(vertx, rule);
            cronHandler.schedule(runner);

            apiHandlers.get(api).add(cronHandler);

            LOGGER.debug(
                "Add health-check for endpoint name[{}] target[{}] with cron[{}]",
                rule.endpoint().getName(),
                rule.endpoint().getTarget(),
                rule.schedule()
            );
        } catch (Exception ex) {
            LOGGER.error("An error occurs while creating an health-check runner", ex);
        }
    }

    private void removeHandlers(Api api) {
        List<EndpointRuleCronHandler> handlers = apiHandlers.remove(api);
        if (handlers != null) {
            LOGGER.debug("Stop health-check for API id[{}] name[{}]", api.getId(), api.getName());
            handlers.forEach(handler -> handler.cancel());
        }
    }

    private void removeHandler(Api api, Endpoint endpoint) {
        List<EndpointRuleCronHandler> cronHandlers = apiHandlers.get(api);
        if (cronHandlers != null) {
            Optional<EndpointRuleCronHandler> endpointCronHandler = cronHandlers
                .stream()
                .filter(handler -> handler.getEndpoint().equals(endpoint))
                .findFirst();

            endpointCronHandler.ifPresent(
                handler -> {
                    LOGGER.debug(
                        "Remove health-check handler id[{}] for endpoint name[{}] type[{}] target[{}]",
                        handler.getTimerId(),
                        endpoint.getName(),
                        endpoint.getType(),
                        endpoint.getTarget()
                    );
                    handler.cancel();
                    cronHandlers.remove(handler);
                }
            );
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
            EndpointRule rule = endpointResolver.resolve(api.getDefinition(), endpoint);
            if (rule != null) {
                addHandler(api, rule);
            }
            return false;
        }

        @Override
        public boolean preRemove(Endpoint endpoint) {
            return false;
        }

        @Override
        public boolean postRemove(Endpoint endpoint) {
            removeHandler(api, endpoint);
            return false;
        }
    }
}
