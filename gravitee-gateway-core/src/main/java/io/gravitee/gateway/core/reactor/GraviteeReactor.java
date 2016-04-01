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
package io.gravitee.gateway.core.reactor;

import io.gravitee.common.event.Event;
import io.gravitee.common.event.EventListener;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.service.AbstractService;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.core.Reactor;
import io.gravitee.gateway.core.definition.Api;
import io.gravitee.gateway.core.event.ApiEvent;
import io.gravitee.gateway.core.reactor.handler.*;
import io.gravitee.gateway.core.reactor.handler.reporter.ReporterHandler;
import io.gravitee.gateway.core.reporter.ReporterService;
import io.gravitee.gateway.core.service.ServiceManager;
import io.gravitee.plugin.core.api.PluginRegistry;
import io.gravitee.plugin.core.internal.PluginEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class GraviteeReactor extends AbstractService implements
        Reactor, EventListener<ApiEvent, Api> {

    private final Logger LOGGER = LoggerFactory.getLogger(GraviteeReactor.class);

    @Autowired
    private EventManager eventManager;

    @Autowired
    @Qualifier("notFoundHandler")
    private ReactorHandler notFoundHandler;

    @Autowired
    private ReactorHandlerManager reactorHandlerManager;

    @Autowired
    private ReporterService reporterService;

    private final ConcurrentMap<String, ReactorHandler> handlers = new ConcurrentHashMap<>();
    private final ConcurrentMap<Api, String> contextPaths = new ConcurrentHashMap<>();

    protected ReactorHandler bestHandler(Request request) {
        StringBuilder path = new StringBuilder(request.path());

        if (path.lastIndexOf("/") < path.length() - 1) {
            path.append('/');
        }

        Set<ReactorHandler> mapHandlers = handlers.entrySet().stream().filter(
                entry -> path.toString().startsWith(entry.getKey())).map(Map.Entry::getValue).collect(Collectors.toSet());

        LOGGER.debug("Found {} handlers for path {}", mapHandlers.size(), path);

        if (!mapHandlers.isEmpty()) {
            ReactorHandler handler = mapHandlers.iterator().next();
            LOGGER.debug("Returning the first handler matching path {} : {}", path, handler);
            return handler;
        }

        return notFoundHandler;
    }

    public void process(Request request, Response response, Handler<Response> handler) {
        LOGGER.debug("Receiving a request {} for path {}", request.id(), request.path());

        ReactorHandler reactorHandler = bestHandler(request);

        // Prepare the handler chain
        handler = new ResponseTimeHandler(request,
                new ReporterHandler(reporterService, request, handler));

        reactorHandler.handle(request, response, handler);
    }

    @Override
    public void onEvent(Event<ApiEvent, Api> event) {
        switch (event.type()) {
            case DEPLOY:
                createHandler(event.content());
                break;
            case UPDATE:
                updateHandler(event.content());
                break;
            case UNDEPLOY:
                removeHandler(event.content());
                break;
        }
    }

    public void createHandler(Api api) {
        if (api.isEnabled()) {
            LOGGER.info("Start deployment in reactor");

            ReactorHandler handler = reactorHandlerManager.create(api);
            try {
                handler.start();
                handlers.putIfAbsent(handler.contextPath(), handler);
                contextPaths.putIfAbsent(api, handler.contextPath());
            } catch (Exception ex) {
                LOGGER.error("Unable to deploy handler", ex);
            }
        } else {
            LOGGER.warn("Api is disabled !");
        }
    }

    public void updateHandler(Api api) {
        String contextPath = contextPaths.get(api);
        if (contextPath != null) {
            ReactorHandler handler = handlers.get(contextPath);
            if (handler != null) {
                removeHandler(api);
                createHandler(api);
            }
        } else {
            createHandler(api);
        }
    }

    public void removeHandler(Api api) {
        String contextPath = contextPaths.remove(api);
        if (contextPath != null) {
            ReactorHandler handler = handlers.remove(contextPath);

            if (handler != null) {
                try {
                    handler.stop();
                    handlers.remove(handler.contextPath());
                    LOGGER.info("API has been removed from reactor");
                } catch (Exception e) {
                    LOGGER.error("Unable to remove handler", e);
                }
            }
        }
    }

    public void clearHandlers() {
        handlers.forEach((s, handler) -> {
            try {
                handler.stop();
                handlers.remove(handler.contextPath());
            } catch (Exception e) {
                LOGGER.error("Unable to remove reactor handler", e);
            }
        });
        contextPaths.clear();
    }

    public void setReactorHandlerManager(ReactorHandlerManager reactorHandlerManager) {
        this.reactorHandlerManager = reactorHandlerManager;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        applicationContext.getBean(PluginEventListener.class).start();
        applicationContext.getBean(PluginRegistry.class).start();

        eventManager.subscribeForEvents(this, ApiEvent.class);

        applicationContext.getBean(ServiceManager.class).start();
        applicationContext.getBean(ReporterService.class).start();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        applicationContext.getBean(PluginRegistry.class).stop();
        applicationContext.getBean(PluginEventListener.class).stop();

        clearHandlers();

        applicationContext.getBean(ServiceManager.class).stop();
        applicationContext.getBean(ReporterService.class).stop();
    }
}
