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

import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.core.Reactor;
import io.gravitee.gateway.core.event.Event;
import io.gravitee.gateway.core.event.EventListener;
import io.gravitee.gateway.core.event.EventManager;
import io.gravitee.gateway.core.manager.ApiEvent;
import io.gravitee.gateway.core.model.Api;
import io.gravitee.gateway.core.model.ApiLifecycleState;
import io.gravitee.gateway.core.reactor.handler.ContextHandler;
import io.gravitee.gateway.core.reactor.handler.ContextHandlerFactory;
import io.gravitee.gateway.core.reactor.handler.Handler;
import io.gravitee.gateway.core.reporter.ReporterService;
import io.gravitee.gateway.core.service.AbstractService;
import io.gravitee.gateway.core.sync.SyncService;
import io.gravitee.plugin.core.api.PluginRegistry;
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
        Reactor<T>, EventListener<ApiEvent, Api>, ApplicationContextAware {

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
    public void onEvent(Event<ApiEvent, Api> event) {
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

    public void addHandler(Api api) {
        if (api.getState() == ApiLifecycleState.STARTED) {
            logger.info("API {} has been enabled in reactor", api.getName());

            ContextHandler handler = contextHandlerFactory.create(api);
            try {
                handler.start();
                handlers.putIfAbsent(handler.getContextPath(), handler);
            } catch (Exception ex) {
                logger.error("Unable to add reactor handler", ex);
            }
        } else {
            logger.warn("Api {} is settled has disable in reactor !", api.getName());
        }
    }

    public void removeHandler(Api api) {
        logger.info("API {} has been disabled (or removed) from reactor", api.getName());

        Handler handler = handlers.remove(api.getPublicURI().getPath());
        if (handler != null) {
            try {
                handler.stop();
                handlers.remove(api.getPublicURI().getPath());
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

        applicationContext.getBean(PluginRegistry.class).start();
        applicationContext.getBean(ReporterService.class).start();

        eventManager.subscribeForEvents(this, ApiEvent.class);

        applicationContext.getBean(SyncService.class).start();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        applicationContext.getBean(PluginRegistry.class).stop();
        applicationContext.getBean(SyncService.class).stop();

        clearHandlers();

        applicationContext.getBean(ReporterService.class).stop();
    }
}
