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
import io.gravitee.gateway.core.Reactor;
import io.gravitee.gateway.core.definition.ApiDefinition;
import io.gravitee.gateway.core.manager.ApiEvent;
import io.gravitee.gateway.core.plugin.PluginEventListener;
import io.gravitee.gateway.core.reactor.handler.ContextHandler;
import io.gravitee.gateway.core.reactor.handler.ContextHandlerFactory;
import io.gravitee.gateway.core.reactor.handler.Handler;
import io.gravitee.gateway.core.registry.LocalApiDefinitionRegistry;
import io.gravitee.gateway.core.reporter.ReporterService;
import io.gravitee.gateway.core.sync.SyncService;
import io.gravitee.plugin.api.PluginRegistry;
import org.eclipse.jetty.http.HttpHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public abstract class GraviteeReactor<T> extends AbstractService implements
        Reactor<T>, EventListener<ApiEvent, ApiDefinition>, ApplicationContextAware {

    private final Logger logger = LoggerFactory.getLogger(GraviteeReactor.class);

    @Autowired
    private EventManager eventManager;

    private ApplicationContext applicationContext;

    @Autowired
    @Qualifier("errorHandler")
    private Handler errorHandler;

    @Autowired
    private ContextHandlerFactory contextHandlerFactory;

    private final ConcurrentMap<String, ContextHandler> handlers = new ConcurrentHashMap();

    protected Handler bestHandler(Request request) {
        String path = request.path();

        Set<ContextHandler> mapHandlers = handlers.entrySet().stream().filter(
                entry -> path.startsWith(entry.getKey())).map(Map.Entry::getValue).collect(Collectors.toSet());

        logger.debug("Found {} handlers for path {}", mapHandlers.size(), path);

        if (! mapHandlers.isEmpty()) {

            // Sort valid handlers and push handler with VirtualHost first
            ContextHandler[] sorted = new ContextHandler[mapHandlers.size()];
            int idx = 0;
            for (ContextHandler handler : mapHandlers) {
                if (handler.hasVirtualHost()) {
                    sorted[idx++] = handler;
                }
            }
            for (ContextHandler handler : mapHandlers) {
                if (!handler.hasVirtualHost()) {
                    sorted[idx++] = handler;
                }
            }

            String host = getHost(request);

            // Always pick-up the first which is corresponding
            for (ContextHandler handler : sorted) {
                if (host.equals(handler.getVirtualHost())) {
                    return handler;
                }
            }

            ContextHandler handler = mapHandlers.iterator().next();
            logger.debug("Returning the first handler matching path {} : {}", path, handler);
            return handler;
        }

        return errorHandler;
    }

    protected T handle(Request request, Response response) {
        return (T) bestHandler(request).handle(request, response);
    }

    private String getHost(Request request) {
        String host = request.headers().get(HttpHeader.HOST.asString());
        if (host == null || host.isEmpty()) {
            return URI.create(request.uri()).getHost();
        } else {
            return host;
        }
    }

    @Override
    public void onEvent(Event<ApiEvent, ApiDefinition> event) {
        switch(event.type()) {
            case CREATE:
                addHandler(event.content());
                break;
            case UPDATE:
                removeHandler(event.content());
                addHandler(event.content());
                break;
            case REMOVE:
                removeHandler(event.content());
                break;
        }
    }

    public void addHandler(ApiDefinition apiDefinition) {
        if (apiDefinition.isEnabled()) {
            logger.info("API {} has been enabled in reactor", apiDefinition.getName());

            ContextHandler handler = contextHandlerFactory.create(apiDefinition);
            try {
                handler.start();
                handlers.putIfAbsent(handler.getContextPath(), handler);
            } catch (Exception ex) {
                logger.error("Unable to add reactor handler", ex);
            }
        } else {
            logger.warn("Api {} is settled has disable in reactor !", apiDefinition.getName());
        }
    }

    public void removeHandler(ApiDefinition apiDefinition) {
        logger.info("API {} has been disabled (or removed) from reactor", apiDefinition.getName());

        Handler handler = handlers.remove(apiDefinition.getProxy().getContextPath());
        if (handler != null) {
            try {
                handler.stop();
                handlers.remove(apiDefinition.getProxy().getContextPath());
            } catch (Exception e) {
                logger.error("Unable to remove reactor handler", e);
            }
        }
    }

    public void clearHandlers() {
        handlers.forEach((s, handler) -> {
            try {
                handler.stop();
                handlers.remove(handler.getContextPath());
            } catch (Exception e) {
                logger.error("Unable to remove reactor handler", e);
            }
        });
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void setContextHandlerFactory(ContextHandlerFactory contextHandlerFactory) {
        this.contextHandlerFactory = contextHandlerFactory;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        applicationContext.getBean(PluginEventListener.class).start();
        applicationContext.getBean(PluginRegistry.class).start();
        applicationContext.getBean(ReporterService.class).start();

        eventManager.subscribeForEvents(this, ApiEvent.class);

        applicationContext.getBean(LocalApiDefinitionRegistry.class).init();
        applicationContext.getBean(SyncService.class).start();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        applicationContext.getBean(PluginRegistry.class).stop();
        applicationContext.getBean(PluginEventListener.class).stop();
        applicationContext.getBean(SyncService.class).stop();

        clearHandlers();

        applicationContext.getBean(ReporterService.class).stop();
    }
}
