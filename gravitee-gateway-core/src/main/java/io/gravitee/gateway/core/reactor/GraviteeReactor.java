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
import io.gravitee.gateway.core.cluster.SyncService;
import io.gravitee.gateway.core.event.Event;
import io.gravitee.gateway.core.event.EventListener;
import io.gravitee.gateway.core.event.EventManager;
import io.gravitee.gateway.core.handler.ContextHandler;
import io.gravitee.gateway.core.handler.Handler;
import io.gravitee.gateway.core.handler.spring.ApiHandlerConfiguration;
import io.gravitee.gateway.core.plugin.PluginManager;
import io.gravitee.gateway.core.reporter.ReporterManager;
import io.gravitee.gateway.core.service.AbstractService;
import io.gravitee.gateway.core.service.ApiLifecycleEvent;
import io.gravitee.gateway.core.service.ApiService;
import io.gravitee.model.Api;
import org.eclipse.jetty.http.HttpHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

import java.net.URI;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public abstract class GraviteeReactor<T> extends AbstractService implements
        Reactor<T>, EventListener<ApiLifecycleEvent, Api>, ApplicationContextAware {

    private final Logger LOGGER = LoggerFactory.getLogger(GraviteeReactor.class);

    @Autowired
    private EventManager eventManager;

    @Autowired
    private ApiService apiService;

    private ApplicationContext applicationContext;

    @Autowired
    @Qualifier("errorHandler")
    private Handler errorHandler;

    private final ConcurrentMap<String, ContextHandler> handlers = new ConcurrentHashMap();
    private final ConcurrentMap<String, ConfigurableApplicationContext> contexts = new ConcurrentHashMap();

    protected Handler getHandler(Request request) {
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

    public void clearHandlers() {
        handlers.forEach((s, contextHandler) -> removeHandler(s));
    }

    protected T handle(Request request, Response response) {
        return (T) getHandler(request).handle(request, response);
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
    public void onEvent(Event<ApiLifecycleEvent, Api> event) {
        switch(event.type()) {
            case START:
                addHandler(event.content());
                break;
            case STOP:
                removeHandler(event.content());
                break;
        }
    }

    public void addHandler(Api api) {
        LOGGER.info("API {} ({}) has been enabled in reactor", api.getName(), api.getVersion());

        AbstractApplicationContext internalApplicationContext = buildApplicationContext(api);
        ContextHandler handler = internalApplicationContext.getBean(ContextHandler.class);

        handlers.putIfAbsent(handler.getContextPath(), handler);
        contexts.putIfAbsent(handler.getContextPath(), internalApplicationContext);
    }

    public void removeHandler(Api api) {
        LOGGER.info("API {} ({}) has been disabled (or removed) from reactor", api.getName(), api.getVersion());
        removeHandler(api.getPublicURI().getPath());
    }

    public void removeHandler(String contextPath) {
        handlers.remove(contextPath);

        ConfigurableApplicationContext internalApplicationContext = contexts.remove(contextPath);
        if (internalApplicationContext != null) {
            internalApplicationContext.close();
        }
    }

    private AbstractApplicationContext buildApplicationContext(Api api) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.setParent(this.applicationContext);

        PropertyPlaceholderConfigurer configurer=new PropertyPlaceholderConfigurer();
        final Properties properties = applicationContext.getBean("graviteeProperties", Properties.class);
        configurer.setProperties(properties);
        configurer.setIgnoreUnresolvablePlaceholders(true);
        context.addBeanFactoryPostProcessor(configurer);

        context.getBeanFactory().registerSingleton("api", api);
        context.register(ApiHandlerConfiguration.class);
        context.setId("context-api-" + api.getName() + "-" + api.getVersion());
        context.refresh();

        return context;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        applicationContext.getBean(PluginManager.class).start();
        applicationContext.getBean(SyncService.class).start();
        applicationContext.getBean(ReporterManager.class).start();

        eventManager.subscribeForEvents(this, ApiLifecycleEvent.class);

        //TODO: Not sure it's the best place to do the following...
        apiService.startAll();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        applicationContext.getBean(PluginManager.class).stop();
        applicationContext.getBean(SyncService.class).stop();

        clearHandlers();

        applicationContext.getBean(ReporterManager.class).stop();
    }
}
